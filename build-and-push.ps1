# build-and-push.ps1
param(
    [string]$ImageName = "adambravo/t1000-bot"
)

$VersionTag = Get-Date -Format "yyyyMMdd-HHmmss"
$LatestTag = "latest"

Write-Host "[INFO] Fazendo login no Docker Hub..." -ForegroundColor Green
docker login

Write-Host "[INFO] Construindo imagem: ${ImageName}:${LatestTag}" -ForegroundColor Green
docker build -t "${ImageName}:${LatestTag}" -t "${ImageName}:${VersionTag}" .

Write-Host "[INFO] Enviando tag ${VersionTag}..." -ForegroundColor Green
docker push "${ImageName}:${VersionTag}"
Write-Host "[INFO] Enviando tag latest..." -ForegroundColor Green
docker push "${ImageName}:${LatestTag}"

Write-Host "[INFO] Concluído! No servidor, rode: docker pull ${ImageName}:latest && ./deploy.sh restart" -ForegroundColor Green