#!/bin/bash

# 部署脚本 - 智能停车场系统
# 用途：执行项目部署，包含回滚逻辑

set -e  # 出错时立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认环境参数
ENV="development"
DEPLOY_DIR="./deploy"
JAR_FILE="target/parking-system-1.0.0.jar"
BACKUP_DIR="./deploy-backup"

# 解析命令行参数
while getopts "e:d:h" opt; do
  case $opt in
    e) ENV="$OPTARG" ;;
    d) DEPLOY_DIR="$OPTARG" ;;
    h) echo "用法: $0 [-e 环境] [-d 部署目录] [-h 帮助]" && exit 0 ;;
    \?) echo "无效选项 -$OPTARG" >&2 && exit 1 ;;
  esac
done

echo -e "${GREEN}开始部署智能停车场系统到 $ENV 环境...${NC}"

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}错误: 找不到JAR文件 $JAR_FILE${NC}"
    echo -e "${YELLOW}请先运行构建脚本: ./scripts/build.sh${NC}"
    exit 1
fi

# 确保部署目录存在
mkdir -p "$DEPLOY_DIR"

# 备份当前部署
if [ -f "$DEPLOY_DIR/parking-system.jar" ]; then
    echo -e "${GREEN}备份当前部署...${NC}"
    mkdir -p "$BACKUP_DIR"
    TIMESTAMP=$(date +"%Y%m%d%H%M%S")
    cp "$DEPLOY_DIR/parking-system.jar" "$BACKUP_DIR/parking-system-$TIMESTAMP.jar"
    if [ -f "$DEPLOY_DIR/application.properties" ]; then
        cp "$DEPLOY_DIR/application.properties" "$BACKUP_DIR/application-$TIMESTAMP.properties"
    fi
fi

# 复制JAR文件到部署目录
echo -e "${GREEN}复制JAR文件到部署目录...${NC}"
cp "$JAR_FILE" "$DEPLOY_DIR/parking-system.jar"

# 复制环境配置文件
if [ -f ".env" ]; then
    echo -e "${GREEN}复制环境配置文件...${NC}"
    cp ".env" "$DEPLOY_DIR/.env"
fi

# 根据环境创建或更新application.properties
if [ "$ENV" = "production" ]; then
    echo -e "${GREEN}配置生产环境属性...${NC}"
    cat > "$DEPLOY_DIR/application.properties" << EOF
spring.profiles.active=prod
logging.level.root=INFO
server.port=8080
EOF
elif [ "$ENV" = "test" ]; then
    echo -e "${GREEN}配置测试环境属性...${NC}"
    cat > "$DEPLOY_DIR/application.properties" << EOF
spring.profiles.active=test
logging.level.root=DEBUG
server.port=8082
EOF
else
    echo -e "${GREEN}配置开发环境属性...${NC}"
    cat > "$DEPLOY_DIR/application.properties" << EOF
spring.profiles.active=dev
logging.level.root=DEBUG
server.port=8080
EOF
fi

# 创建启动脚本
echo -e "${GREEN}创建启动脚本...${NC}"
cat > "$DEPLOY_DIR/start.sh" << EOF
#!/bin/bash
java -jar parking-system.jar --spring.config.location=application.properties
EOF
chmod +x "$DEPLOY_DIR/start.sh"

# 创建停止脚本
echo -e "${GREEN}创建停止脚本...${NC}"
cat > "$DEPLOY_DIR/stop.sh" << EOF
#!/bin/bash
ps aux | grep "parking-system.jar" | grep -v grep | awk '{print \$2}' | xargs kill -9 2>/dev/null || echo "应用未运行"
EOF
chmod +x "$DEPLOY_DIR/stop.sh"

# 创建回滚脚本
echo -e "${GREEN}创建回滚脚本...${NC}"
cat > "$DEPLOY_DIR/rollback.sh" << EOF
#!/bin/bash

if [ -f "../deploy-backup/parking-system-*.jar" ]; then
    LATEST_BACKUP=\$(ls -t ../deploy-backup/parking-system-*.jar | head -1)
    echo "回滚到最新备份: \$LATEST_BACKUP"
    
    # 停止当前应用
    ./stop.sh
    
    # 恢复备份
    cp "\$LATEST_BACKUP" parking-system.jar
    
    # 恢复配置文件
    BACKUP_TIMESTAMP=\$(echo "\$LATEST_BACKUP" | grep -oE '[0-9]{14}')
    if [ -f "../deploy-backup/application-\${BACKUP_TIMESTAMP}.properties" ]; then
        cp "../deploy-backup/application-\${BACKUP_TIMESTAMP}.properties" application.properties
    fi
    
    # 重启应用
    echo "重启应用..."
    ./start.sh &
else
    echo "没有找到备份文件"
    exit 1
fi
EOF
chmod +x "$DEPLOY_DIR/rollback.sh"

echo -e "${GREEN}部署完成！${NC}"
echo -e "${GREEN}部署目录: $DEPLOY_DIR${NC}"
echo -e "${GREEN}启动应用: cd $DEPLOY_DIR && ./start.sh${NC}"
echo -e "${GREEN}停止应用: cd $DEPLOY_DIR && ./stop.sh${NC}"
echo -e "${GREEN}回滚应用: cd $DEPLOY_DIR && ./rollback.sh${NC}"

# 显示部署信息
echo -e "${YELLOW}\n部署信息:${NC}"
echo -e "- 环境: $ENV"
echo -e "- 应用版本: 1.0.0"
echo -e "- 部署时间: $(date +"%Y-%m-%d %H:%M:%S")"

# 部署后提示
if [ "$ENV" = "production" ]; then
    echo -e "\n${YELLOW}生产环境部署注意事项:${NC}"
    echo -e "1. 请确保已进行全面的测试"
    echo -e "2. 建议在非高峰期进行部署"
    echo -e "3. 部署后请验证系统功能正常"
fi