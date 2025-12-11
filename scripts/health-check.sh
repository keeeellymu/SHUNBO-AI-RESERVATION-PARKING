#!/bin/bash

# 健康检查脚本 - 智能停车场系统
# 用途：验证服务是否正常运行

set -e  # 出错时立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认参数
SERVICE_URL="http://localhost:8080"
HEALTH_ENDPOINT="/actuator/health"
TIMEOUT=300  # 默认超时时间（秒）
INTERVAL=5   # 检查间隔（秒）
SERVICE_NAME="parking-service"

# 解析命令行参数
while getopts "u:e:t:i:s:h" opt; do
  case $opt in
    u) SERVICE_URL="$OPTARG" ;;
    e) HEALTH_ENDPOINT="$OPTARG" ;;
    t) TIMEOUT="$OPTARG" ;;
    i) INTERVAL="$OPTARG" ;;
    s) SERVICE_NAME="$OPTARG" ;;
    h) echo "用法: $0 [-u 服务URL] [-e 健康检查端点] [-t 超时时间] [-i 检查间隔] [-s 服务名称] [-h 帮助]" && exit 0 ;;
    \?) echo "无效选项 -$OPTARG" >&2 && exit 1 ;;
  esac
done

FULL_URL="${SERVICE_URL}${HEALTH_ENDPOINT}"

# 日志函数
log() {
  echo -e "[$(date +"%Y-%m-%d %H:%M:%S")] $1"
}

echo -e "${GREEN}开始对 $SERVICE_NAME 进行健康检查...${NC}"
log "服务URL: $FULL_URL"
log "超时时间: ${TIMEOUT}秒"
log "检查间隔: ${INTERVAL}秒"

# 检查curl是否安装
if ! command -v curl &> /dev/null; then
    log "${RED}错误: curl 命令未找到，请安装后重试${NC}"
    exit 1
fi

# 开始计时
START_TIME=$(date +%s)
ELAPSED=0

# 进行健康检查
while [ $ELAPSED -lt $TIMEOUT ]; do
    log "尝试连接到服务..."
    
    # 发送健康检查请求
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$FULL_URL" 2>/dev/null || echo "error")
    
    if [ "$RESPONSE" = "200" ]; then
        log "${GREEN}健康检查成功！服务运行正常${NC}"
        log "服务 $SERVICE_NAME 已成功启动并可用"
        exit 0
    elif [ "$RESPONSE" = "error" ]; then
        log "${YELLOW}连接失败，服务可能尚未启动，等待 ${INTERVAL}秒后重试...${NC}"
    else
        log "${YELLOW}服务返回状态码: $RESPONSE，等待 ${INTERVAL}秒后重试...${NC}"
    fi
    
    # 等待指定间隔后重试
    sleep $INTERVAL
    
    # 更新已用时间
    ELAPSED=$(($(date +%s) - START_TIME))
    log "已等待 ${ELAPSED}/${TIMEOUT} 秒"
done

# 超时处理
log "${RED}健康检查失败！服务在 ${TIMEOUT}秒内未正常启动${NC}"
log "请检查服务日志以获取更多信息"
exit 1