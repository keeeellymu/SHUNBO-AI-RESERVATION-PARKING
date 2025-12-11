#!/bin/bash

# 部署前检查脚本 - 智能停车场系统
# 用途：在部署前执行环境和配置验证

set -e  # 出错时立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认环境参数
ENV="development"

# 解析命令行参数
while getopts "e:h" opt; do
  case $opt in
    e) ENV="$OPTARG" ;;
    h) echo "用法: $0 [-e 环境] [-h 帮助]" && exit 0 ;;
    \?) echo "无效选项 -$OPTARG" >&2 && exit 1 ;;
  esac
done

# 日志函数
log() {
  echo -e "[$(date +"%Y-%m-%d %H:%M:%S")] $1"
}

echo -e "${GREEN}开始部署前检查...${NC}"
log "目标环境: $ENV"

# 检查JDK版本
log "检查JDK版本..."
JAVA_VERSION=$(java -version 2>&1 | grep -i version | awk '{print $3}' | sed 's/"//g')
if [[ $JAVA_VERSION == 11* ]]; then
    log "${GREEN}JDK版本检查通过: $JAVA_VERSION${NC}"
else
    log "${RED}JDK版本检查失败: 请使用JDK 11版本，当前版本为 $JAVA_VERSION${NC}"
    exit 1
fi

# 检查Maven版本
log "检查Maven版本..."
MAVEN_VERSION=$(mvn -v | grep -i "maven home" | cut -d ' ' -f 3)
if [[ ! -z "$MAVEN_VERSION" ]]; then
    log "${GREEN}Maven检查通过${NC}"
else
    log "${RED}Maven检查失败: 未找到Maven${NC}"
    exit 1
fi

# 检查环境变量文件
log "检查环境变量文件..."
if [ ! -f ".env" ]; then
    log "${RED}环境变量文件检查失败: .env 文件不存在${NC}"
    if [ -f ".env.example" ]; then
        log "${YELLOW}提示: 可以从 .env.example 创建 .env 文件${NC}"
    fi
    exit 1
else
    log "${GREEN}环境变量文件检查通过${NC}"
    
    # 验证必需的环境变量
    REQUIRED_ENVS=("DB_HOST" "DB_PORT" "DB_DATABASE" "DB_USERNAME" "REDIS_HOST" "REDIS_PORT" "AMAP_API_KEY")
    MISSING_ENVS=()
    
    for env_var in "${REQUIRED_ENVS[@]}"; do
        if ! grep -q "^$env_var=" .env; then
            MISSING_ENVS+=("$env_var")
        fi
    done
    
    if [ ${#MISSING_ENVS[@]} -ne 0 ]; then
        log "${YELLOW}警告: .env 文件中缺少以下必需的环境变量:${NC}"
        for env_var in "${MISSING_ENVS[@]}"; do
            log "  - $env_var"
        done
    else
        log "${GREEN}必需环境变量检查通过${NC}"
    fi
fi

# 检查构建产物
log "检查构建产物..."
if [ ! -f "target/parking-system-1.0.0.jar" ]; then
    log "${YELLOW}构建产物检查警告: 未找到JAR文件，请先运行构建${NC}"
else
    log "${GREEN}构建产物检查通过${NC}"
fi

# 根据环境执行特定检查
if [ "$ENV" = "production" ]; then
    log "执行生产环境特定检查..."
    
    # 检查应用调试模式是否关闭
    if grep -q "^APP_DEBUG=\"true\"" .env; then
        log "${RED}生产环境检查失败: APP_DEBUG 必须设置为 false${NC}"
        exit 1
    else
        log "${GREEN}调试模式检查通过${NC}"
    fi
    
    # 检查日志级别
    if grep -q "^LOG_LEVEL=\"debug\"" .env; then
        log "${YELLOW}警告: 生产环境建议使用 info 或更高的日志级别${NC}"
    else
        log "${GREEN}日志级别检查通过${NC}"
    fi
elif [ "$ENV" = "test" ]; then
    log "执行测试环境特定检查..."
    # 可以添加测试环境特定的检查
fi

# 检查脚本权限
log "检查脚本权限..."
for script in build.sh deploy.sh health-check.sh; do
    if [ -f "scripts/$script" ] && [ ! -x "scripts/$script" ]; then
        log "${YELLOW}脚本权限警告: scripts/$script 缺少执行权限${NC}"
    fi
done

log "${GREEN}部署前检查完成！${NC}"
exit 0