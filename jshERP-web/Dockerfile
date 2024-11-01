# 使用官方 Node.js 镜像作为基础镜像
FROM node:14

# 安装指定版本的 Yarn
RUN npm install -g yarn@1.22.5

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY . .

# 安装项目依赖
RUN yarn install

# 启动命令
CMD ["yarn", "serve"]
