# CMS

销售团队客户接触登记工具第一版。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、PWA
- 后端：Java 21、Spring Boot、Spring Security、Spring Data JPA
- 数据库：SQLite

## 本地启动

后端：

```bash
cd backend
./mvnw spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

访问：`http://127.0.0.1:5173/`

## 本地种子管理者

- 手机号：`13800000000`
- 密码：`admin123456`

这个账号同时拥有销售成员和管理者角色，用于创建第一批成员账号。

## 验证

后端测试：

```bash
cd backend
./mvnw test
```

前端构建：

```bash
cd frontend
npm run build
```
