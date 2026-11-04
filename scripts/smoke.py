"""End-to-end verification against a running Compose stack; no third-party packages."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.request
import uuid

parser = argparse.ArgumentParser()
parser.add_argument('--base', default='http://localhost:8080')
parser.add_argument('--resilience', action='store_true', help='Stop/restart local Compose dependencies during verification')
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
config = {}
if (root / '.env').exists():
    config = dict(line.split('=', 1) for line in (root / '.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
admin_password = os.environ.get('ADMIN_PASSWORD', config.get('ADMIN_PASSWORD'))
if not admin_password:
    raise SystemExit('Set ADMIN_PASSWORD or generate .env first')


def request(method, path, body=None, token=None, expected=200, key=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if key:
        headers['Idempotency-Key'] = key
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(args.base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=12) as response:
            status, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read()
    if status != expected:
        raise AssertionError(f'{method} {path}: expected {expected}, got {status}')
    return json.loads(raw) if raw else None


def wait_until(action, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            result = action()
            if result:
                return result
        except (OSError, AssertionError, urllib.error.URLError):
            pass
        time.sleep(2)
    raise AssertionError('Timed out waiting for expected service state')


def compose(*commands):
    subprocess.run(['docker', 'compose', *commands], cwd=root, check=True)


def login(username, password):
    return request('POST', '/api/v1/auth/login', {'username': username, 'password': password})['accessToken']


def register(prefix):
    username = prefix + uuid.uuid4().hex[:12]
    password = uuid.uuid4().hex
    account = request('POST', '/api/v1/auth/register', {'username': username, 'password': password}, expected=201)
    return account['id'], login(username, password)


wait_until(lambda: request('GET', '/actuator/health/readiness')['status'] == 'UP', timeout=180)
admin = login('admin', admin_password)
client_id, client = register('client')
staff_id, staff = register('staff')
other_id, other = register('other')
request('PATCH', f'/api/v1/admin/users/{staff_id}/role', {'role': 'SUPPORT'}, admin)
request('GET', '/v3/api-docs')
key = uuid.uuid4().hex
draft = {'title': 'Invoice discrepancy', 'description': 'Please investigate', 'priority': 'HIGH', 'tagIds': [1]}
ticket = request('POST', '/api/v1/tickets', draft, client, 201, key)
ticket_id = ticket['id']
assert request('POST', '/api/v1/tickets', draft, client, 201, key)['id'] == ticket_id
request('GET', f'/api/v1/tickets/{ticket_id}', token=other, expected=404)
request('PATCH', f'/api/v1/tickets/{ticket_id}/assignee', {'assigneeId': staff_id}, admin)
request('POST', f'/api/v1/tickets/{ticket_id}/comments', {'body': 'Internal review', 'internal': True}, staff, 201)
assert request('GET', f'/api/v1/tickets/{ticket_id}/comments', token=client)['total'] == 0
for status, token in [('IN_PROGRESS', staff), ('RESOLVED', staff), ('CLOSED', client)]:
    request('PATCH', f'/api/v1/tickets/{ticket_id}/status', {'status': status}, token)
assert len(request('GET', f'/api/v1/tickets/{ticket_id}/history', token=client)) == 4
request('GET', '/api/v1/reports/summary', token=staff)
request('GET', '/api/v1/reports/workload', token=staff)


def delivered(ticket, status):
    inbox = request('GET', '/api/v1/notifications?size=100', token=client)
    return any(item['ticketId'] == ticket and item['status'] == status for item in inbox['items'])


wait_until(lambda: delivered(ticket_id, 'CLOSED'))
assert request('GET', '/api/v1/notifications', token=other)['items'] == []
print('PASS: JWT roles, isolation, idempotency, ticket lifecycle, audit, RabbitMQ delivery and REST inbox')

if args.resilience:
    try:
        compose('stop', 'redis')
        request('GET', '/api/v1/tags', token=client)
        print('PASS: Redis outage falls back to PostgreSQL')
    finally:
        compose('start', 'redis')
    try:
        compose('stop', 'rabbitmq')
        deferred = request('POST', '/api/v1/tickets', draft, client, 201, uuid.uuid4().hex)['id']
        request('GET', f'/api/v1/tickets/{deferred}', token=client)
        time.sleep(4)
    finally:
        compose('start', 'rabbitmq')
    wait_until(lambda: delivered(deferred, 'NEW'), timeout=120)
    print('PASS: RabbitMQ outage preserves ticket and outbox; delivery resumes')
    try:
        compose('stop', 'worker')
        request('GET', '/api/v1/notifications', token=client, expected=503)
        request('GET', f'/api/v1/tickets/{ticket_id}', token=client)
    finally:
        compose('start', 'worker')
    wait_until(lambda: delivered(ticket_id, 'CLOSED'))
    print('PASS: worker outage is isolated; inbox recovers')
