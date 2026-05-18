# 使用 Sales Member Activity Calendar 作为接触计划规则的事实来源

第一版的 **Planned Contact**、**Customer Contact**、**Registered Planned Contact**、**Unregistered Planned Contact**、今日列表、管理端计划执行统计和高关注长期未接触列表，都会反复判断同一件事：某个 **Sales Member** 在某个 **Business Date** 对某个客户是否计划接触、是否已经通过当天的 **Customer Contact** 登记。我们决定以 **Sales Member Activity Calendar** 作为这些接触计划规则的事实来源，而不是让客户详情、今日列表、管理统计分别计算自己的计划状态。

**Registered Planned Contact** 是从同一 **Sales Member**、同一客户、同一 **Business Date** 是否存在第一条 **Customer Contact** 派生出来的事实，不在 **Planned Contact** 上回写状态字段。**Planned Contact** 的唯一性由模块显式检查并由数据库唯一约束兜底，约束为同一 **Sales Member**、同一客户、同一计划日期最多一条。**Business Date** 统一使用 Asia/Shanghai 自然日，从 **Contact Time** 派生，避免服务器时区、浏览器时区或 UTC 日期导致今天列表和登记状态错位。

**Customer Contact** 提交人只从当前登录的 **User Account** 推导，不允许请求体传入 Sales Member；管理者不能代销售成员提交接触记录。提交 **Customer Contact** 是独立动作，只写入不可变接触记录，并让登记状态可被 Calendar 派生；提交后的“创建下一次 Planned Contact”和“顺手调整 Attention Level”是后续独立动作，不和接触记录提交绑在一个事务里。

当管理者变更客户 **Owner** 时，Customer governance 负责验证管理权限和新 **Owner** 是否为可用 **Sales Member**，但删除旧 **Owner** 今天及未来 **Planned Contacts** 的规则由 **Sales Member Activity Calendar** 执行。客户详情只展示未来 **Planned Contacts** 和实际发生过的 **Customer Contacts**；过去的 **Unregistered Planned Contacts** 保留在 Calendar 的日期视图和管理端计划执行统计里，不混入客户历史。
