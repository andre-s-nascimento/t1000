#!/bin/bash

# ==============================
# 🔧 CONFIGURAÇÕES
# ==============================
APP_NAME="t1000-bot"
IMAGE_NAME="adambravo/t1000-bot"
IMAGE_TAG="latest"
DOCKER_IMAGE="$IMAGE_NAME:$IMAGE_TAG"

DATA_PATH="$(pwd)/temp_audio"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ==============================
# 📋 FUNÇÕES DE LOG
# ==============================
log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ==============================
# 🔍 VALIDAÇÕES
# ==============================
check_docker() {
    if ! command -v docker &>/dev/null; then
        log_error "Docker não está instalado ou não está no PATH"
        exit 1
    fi
}

load_env() {
    if [ -f .env ]; then
        set -a
        source .env
        set +a
        log_info "Variáveis carregadas do .env"
    else
        log_error "Arquivo .env não encontrado!"
        exit 1
    fi
}

# ==============================
# 🛠 BUILD
# ==============================
build_image() {
    log_info "Construindo imagem Docker: $DOCKER_IMAGE"
    docker build --pull -t "$DOCKER_IMAGE" .
    if [ $? -ne 0 ]; then
        log_error "Falha no build da imagem"
        exit 1
    fi
}

# ==============================
# 🧹 LIMPEZA
# ==============================
stop_container() {
    log_info "Parando container antigo (se existir)..."
    docker stop "$APP_NAME" &>/dev/null || true
    docker rm "$APP_NAME" &>/dev/null || true
}

# ==============================
# 🚀 RUN
# ==============================
run_container() {
    log_info "Iniciando container do $APP_NAME"

    mkdir -p "$DATA_PATH"

    docker run -d \
        --name "$APP_NAME" \
        --restart unless-stopped \
        --env-file .env \
        -v "$DATA_PATH:/app/temp_audio" \
        "$DOCKER_IMAGE"

    if [ $? -eq 0 ]; then
        log_info "Container iniciado com sucesso!"
    else
        log_error "Erro ao iniciar container"
        exit 1
    fi
}

# ==============================
# 📊 STATUS & LOGS
# ==============================
show_status() {
    docker ps --filter "name=$APP_NAME"
}

show_logs() {
    docker logs --tail 50 "$APP_NAME"
}

logs_follow() {
    docker logs -f "$APP_NAME"
}

# ==============================
# 🚀 MAIN
# ==============================
main() {
    echo "========================================="
    echo "🤖 Deploy Automatizado - $APP_NAME"
    echo "========================================="

    check_docker
    load_env

    case "${1:-deploy}" in
        deploy)
            build_image
            stop_container
            run_container
            show_status
            show_logs
            ;;
        restart)
            stop_container
            run_container
            show_status
            ;;
        stop)
            stop_container
            log_info "Container parado"
            ;;
        logs)
            logs_follow
            ;;
        status)
            show_status
            ;;
        *)
            echo "Uso: $0 {deploy|restart|stop|logs|status}"
            exit 1
            ;;
    esac
}

main "$@"