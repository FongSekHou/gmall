# <p align="center">gmall入门手册</p> 

## 1. gmall各模块服务的端口号
+ gmall-**user-service**用户服务的service层端口是**8070** 
+ gmall-**user-web**用户服务的web层端口是**8080**  
+ gmall-**manage-service**后台管理服务的service层端口是**8071**
+ gmall-**manage-web**后台管理服务的web层端口是**8081**
+ gmall-**item-web**商品详情服务的web层端口是**8082**
+ gmall-**search-service**查询服务的service层端口是**8073**
+ gmall-**search-web**查询服务的web层端口是**8083**
+ gmall-**cart-service**购物车服务的service层端口是**8074**
+ gmall-**cart-web**购物车服务的web层端口是**8084**
+ gmall-**passport-web**用户认证服务的web层端口是**8085**
+ gmall-**order-service**订单服务的service层端口是**8076**
+ gmall-**order-web**订单服务的web层端口是**8086**
+ gmall-**payment-service**支付服务的service层端口是**8077**
+ gmall-**payment-web**支付服务的web层端口是**8087**
## 2. 未完成功能
1. 搭建fastdfs环境完成图片上传功能
2. 社交登录时，第三方网站中的账户数据和本网站存储的账号数据的不一致性（因为本网站存储的是第一次登录时的数据，后面第三方网站的账户数据更改了也无法同步）
解决：本网站提供个人中心页面支持`一键同步数据`或者`自行修改`
3. 拦截器放行bug：将启动类拖至父级目录下或在启动类上加注解`@ComponentScan({"com.atguigu.gmall"})`即可解决，原因未知
4. 单点登录将token存入缓存的用处是什么？存不存看具体需求，网上的结论也是众说纷纭，有些人说要存，有些人说不存
5. 社交登录时rpc主键自增策略失败。问题描述：认证服务调用用户service服务插入用户数据后无法获得用户数据自增的主键  
原因：mybatis的主键返回策略无法跨越rpc  
解决方案：
```java
    //插入数据的方法返回值为插入的数据对象
    @Override
    public UmsMember saveMember(UmsMember umsMember) {
        try {
            userMapper.insertSelective(umsMember);
            return umsMember;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    //插入数据后再手动获取对象获取值
    UmsMember umsMember = userService.saveMember(umsMember);
```
6. 订单的概念是什么？是一堆商品的集合吗？taobao中一次性购买多件商品，每家店铺的商品集合对应不同的订单
7. redis和activemq在springboot都有默认的整合（starter），不用自己写util类获取，有内置的template对象使用
## 3.bug
1. [小米6手机详情页](http://127.0.0.1:8082/11.html)当前skuid为11，将容量切换到6G+128G后当前skuid为21，再将颜色先切换到陶瓷黑再切换回亮蓝，此时的skuid为15，不是原来的21了  

## 4. 项目需要继续完成的点
1. 后台管理，保存商品sku时用消息队列同步缓存中的数据和同步es中的数据（虽然保存至mysql和redis的操作都是在skuServiceImpl中进行，但是不能因为保存redis失败而回滚mysql中的数据，因此要拆开来写，解耦）
2. 搜索商品时的热度值字段：如果每次一个商品被搜索以后都去es中改字段的值，那对es的负担太大了，所以将新建热度值字段专门存入redis，对应es中的热度值，在es检索出结果之后根据redis的热度值排序；稀释es的操作次数：比如更新了redis数据100次后就前去es修改热度值字段，具体稀释程度视情况而定。
3. 购物车模块：当用户登录时合并cookie和db的购物车数据并同步redis（消息队列）；访问购物车列表时如果已登录就删除cookie的购物车
4. 提交订单时两个强一致性的字段（价格、库存）：调用库存服务的查询接口做库存的校验，验价已完成
5. 库存削减的队列：当锁定库存后，发送消息通知订单服务修改订单状态为准备出库



