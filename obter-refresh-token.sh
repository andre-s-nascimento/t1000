#!/bin/bash
# =============================================================================
# SCRIPT DE AUTORIZAÇÃO ÚNICA — Google Blogger API
# Execute este script UMA VEZ no seu computador local para obter o refresh_token.
# Depois salve o token como variável de ambiente GOOGLE_REFRESH_TOKEN no servidor.
# =============================================================================
#
# PRÉ-REQUISITOS:
#   1. Acesse: https://console.cloud.google.com/
#   2. Crie um projeto (ou use um existente)
#   3. Ative a API "Blogger API v3"
#   4. Em "Credenciais" → "Criar credenciais" → "ID do cliente OAuth 2.0"
#      - Tipo: Aplicativo da Web
#      - URIs de redirecionamento autorizados: http://localhost:8888/callback
#   5. Copie o Client ID e Client Secret abaixo
#
# =============================================================================

CLIENT_ID="SEU_GOOGLE_CLIENT_ID_AQUI"
CLIENT_SECRET="SEU_GOOGLE_CLIENT_SECRET_AQUI"
REDIRECT_URI="http://localhost:8888/callback"
SCOPE="https://www.googleapis.com/auth/blogger"

echo ""
echo "========================================"
echo " PASSO 1: Abra esta URL no navegador"
echo "========================================"
echo ""
AUTH_URL="https://accounts.google.com/o/oauth2/auth?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code&scope=${SCOPE}&access_type=offline&prompt=consent"
echo "$AUTH_URL"
echo ""
echo "Após autorizar, o Google vai redirecionar para:"
echo "  http://localhost:8888/callback?code=CODIGO_AQUI"
echo ""

read -p "Cole aqui o 'code' que apareceu na URL de redirecionamento: " AUTH_CODE

echo ""
echo "========================================"
echo " PASSO 2: Trocando code por refresh_token"
echo "========================================"

RESPONSE=$(curl -s -X POST "https://oauth2.googleapis.com/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "code=${AUTH_CODE}" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" \
  -d "redirect_uri=${REDIRECT_URI}" \
  -d "grant_type=authorization_code")

echo ""
echo "Resposta completa da API:"
echo "$RESPONSE"
echo ""

REFRESH_TOKEN=$(echo "$RESPONSE" | grep -o '"refresh_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$REFRESH_TOKEN" ]; then
  echo "❌ Não foi possível extrair o refresh_token. Verifique a resposta acima."
  exit 1
fi

echo "========================================"
echo " ✅ REFRESH TOKEN OBTIDO COM SUCESSO!"
echo "========================================"
echo ""
echo "Adicione esta variável de ambiente no seu servidor/docker-compose:"
echo ""
echo "  GOOGLE_REFRESH_TOKEN=${REFRESH_TOKEN}"
echo ""
echo "E também:"
echo "  GOOGLE_CLIENT_ID=${CLIENT_ID}"
echo "  GOOGLE_CLIENT_SECRET=${CLIENT_SECRET}"
echo ""
echo "Para encontrar o BLOGGER_BLOG_ID:"
echo "  Acesse: https://www.blogger.com/blog/posts/SEU_BLOG"
echo "  O número longo na URL é o seu Blog ID."
echo ""