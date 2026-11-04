$ErrorActionPreference='Stop'
$target=Join-Path $PSScriptRoot '../.env'
if(Test-Path $target){throw '.env already exists; refusing to replace secrets'}
$lines=foreach($name in @('DB_PASSWORD','RABBIT_PASSWORD','REDIS_PASSWORD','JWT_SECRET','INTERNAL_TOKEN','ADMIN_PASSWORD')) {
 $bytes=New-Object byte[] 32
 $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
 try {$rng.GetBytes($bytes)} finally {$rng.Dispose()}
 $value=([BitConverter]::ToString($bytes)).Replace('-','').ToLowerInvariant()
 "$name=$value"
}
[IO.File]::WriteAllLines([IO.Path]::GetFullPath($target),$lines)
Write-Output 'Created .env with random secrets. Read ADMIN_PASSWORD locally to sign in as admin.'
