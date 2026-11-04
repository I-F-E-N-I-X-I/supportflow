-- Read-only exercises against the API database unless wrapped in the rollback block below.
SELECT id,title,status FROM ticket WHERE priority='HIGH' ORDER BY created_at DESC LIMIT 20;
SELECT t.id,u.username AS client FROM ticket t INNER JOIN app_user u ON u.id=t.client_id;
SELECT t.id,a.username AS assignee FROM ticket t LEFT JOIN app_user a ON a.id=t.assignee_id;
SELECT status,COUNT(*) AS total,SUM(compensation),AVG(compensation),MIN(created_at),MAX(updated_at)
FROM ticket GROUP BY status HAVING COUNT(*)>=1;
SELECT id,title FROM ticket WHERE assignee_id IS NULL;
SELECT u.id FROM app_user u WHERE EXISTS (SELECT 1 FROM ticket t WHERE t.client_id=u.id AND t.status='NEW');
SELECT id FROM ticket WHERE compensation > (SELECT AVG(compensation) FROM ticket);
-- Run mutations ONLY in a disposable learning database; ROLLBACK prevents persistence.
BEGIN;
INSERT INTO tag(name) VALUES ('sql-practice-only');
UPDATE tag SET name='sql-practice-renamed' WHERE name='sql-practice-only';
DELETE FROM tag WHERE name='sql-practice-renamed';
ROLLBACK;
-- Inspect a plan; ANALYZE really executes the SELECT.
EXPLAIN ANALYZE SELECT id FROM ticket WHERE client_id=1 ORDER BY created_at DESC LIMIT 20;
