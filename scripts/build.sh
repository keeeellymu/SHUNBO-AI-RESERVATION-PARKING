#!/bin/bash

# 构建脚本 - 智能停车场系统
# 用途：执行项目构建，生成可部署的应用包

set -e  # 出错时立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}开始构建智能停车场系统...${NC}"

# 检查环境变量文件
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}警告: .env 文件不存在，请确保已配置环境变量${NC}"
    if [ -f ".env.example" ]; then
        echo -e "${YELLOW}正在从 .env.example 创建 .env 文件...${NC}"
        cp .env.example .env
        echo -e "${YELLOW}请编辑 .env 文件并配置必要的环境变量${NC}"
    fi
fi

# 清理旧的构建产物
echo -e "${GREEN}清理旧的构建产物...${NC}"
mvn clean

# 执行依赖检查
echo -e "${GREEN}执行依赖安全检查...${NC}"
mvn org.owasp:dependency-check-maven:check || echo -e "${YELLOW}依赖检查完成，部分依赖可能存在安全风险${NC}"

# 执行构建
echo -e "${GREEN}执行Maven构建...${NC}"
mvn package -DskipTests

# 验证构建结果
if [ -f "target/parking-system-1.0.0.jar" ]; then
    echo -e "${GREEN}构建成功！生成的JAR包路径: target/parking-system-1.0.0.jar${NC}"
    
    # 生成构建信息
    echo -e "${GREEN}生成构建信息...${NC}"
    BUILD_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    echo "{\"version\": \"1.0.0\", \"buildTime\": \"$BUILD_TIME\", \"commitId\": \"$GIT_COMMIT\"}" > target/build-info.json
    
    echo -e "${GREEN}构建完成！${NC}"
    echo -e "${GREEN}JAR文件: target/parking-system-1.0.0.jar${NC}"
    echo -e "${GREEN}构建信息: target/build-info.json${NC}"
else
    echo -e "${RED}构建失败！未找到生成的JAR文件${NC}"
    exit 1
fi