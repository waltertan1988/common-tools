# 平滑迁移数据库的分片数据
MySQL平滑迁移数据到分库分表中

## 设计
![Pandao editor.md](https://github.com/waltertan1988/common-tools/blob/main/database-sharding-migration/docs/design.drawio.png?raw=true "design.png")  
如上图，一共分为3个阶段、2种用户角色（真实用户与测试用户）、3种数据库（旧db、临时db、新db）和新旧版本的app应用。

* 阶段1：对旧DB数据进行一次全量迁移，新旧DB同时工作、仅旧版本app工作  
此阶段下，所有用户都使用旧版app
```
# 全量导出数据（含binlog位置）
mysqldump -uroot -p el_shop_emall orders --single-transaction --flush-logs --master-data > el_shop_emall.orders.sql

# 全量导入数据
mysql -uroot -p el_shop_emall < dump.el_shop_emall.orders.sql
```

* 阶段2：对旧DB的增量数据进行持续迁移，新旧DB、新旧版本app同时工作  
此阶段下，真实用户使用旧app，测试用户使用新版app

* 阶段3：下线旧DB和旧应用  
此阶段下，所有用户都使用新版app

## 开始使用
* Step1: 启动SpringBoot应用进程：com.walter.dsm.Application  
* Step2: DML触发el_shop_emall.orders表的变更  

## 其他参考
* [Canal Client使用范例](https://github.com/waltertan1988/common-tools/blob/main/database-sharding-migration/src/test/java/com/walter/dsm/ApplicationTest.java)  
