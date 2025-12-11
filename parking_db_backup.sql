-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: parking_db
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `parking_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `parking_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `parking_db`;

--
-- Table structure for table `parking_lot`
--

DROP TABLE IF EXISTS `parking_lot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parking_lot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '停车场ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '停车场名称',
  `address` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地址',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_spaces` int NOT NULL DEFAULT '0' COMMENT '总车位数',
  `available_spaces` int NOT NULL DEFAULT '0' COMMENT '可用车位数',
  `hourly_rate` decimal(10,2) NOT NULL DEFAULT '5.00' COMMENT '每小时收费',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open' COMMENT '状态 open/closed',
  `operating_hours` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '00:00-24:00' COMMENT '营业时间',
  `longitude` double DEFAULT NULL COMMENT '经度',
  `latitude` double DEFAULT NULL COMMENT '纬度',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '停车场图片URL',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_location` (`longitude`,`latitude`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='停车场信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parking_lot`
--

LOCK TABLES `parking_lot` WRITE;
/*!40000 ALTER TABLE `parking_lot` DISABLE KEYS */;
INSERT INTO `parking_lot` VALUES (1,'太古汇停车场','广州市天河区天河路383号','天河区',500,10,12.00,'open','07:00-23:00',113.331394,23.137466,'2025-11-18 21:18:13','2025-11-24 10:10:57','https://i.ibb.co/99kmCZWx/taiguhui.jpg'),(2,'正佳广场停车场','广州市天河区天河路228号','天河区',795,5,10.00,'open','07:00-23:00',113.330194,23.136566,'2025-11-18 21:18:13','2025-11-24 13:57:33','https://i.ibb.co/qYknn7wJ/zhengjia-square.jpg'),(3,'天河城停车场','广州市天河区天河路208号','天河区',596,2,11.00,'open','07:00-23:00',113.329894,23.135866,'2025-11-18 21:18:13','2025-11-24 12:38:29','https://i.ibb.co/svvTwhG9/tianhemall.jpg'),(4,'万菱汇停车场','广州市天河区天河路230号','天河区',398,3,10.00,'open','07:00-23:00',113.330394,23.136266,'2025-11-18 21:18:13','2025-12-05 09:22:19','https://i.ibb.co/QvcqXvzb/wanlinghui.jpg'),(5,'广州塔停车场','广州市海珠区阅江西路222号','海珠区',297,2,8.00,'open','09:00-22:00',113.324944,23.106594,'2025-11-18 21:18:13','2025-11-24 14:06:46','https://i.ibb.co/sv4RB2Dh/cantontower.jpg'),(6,'北京路停车场','广州市越秀区北京路283号','越秀区',249,4,8.00,'open','08:00-22:00',113.267194,23.124266,'2025-11-18 21:18:13','2025-11-24 10:48:51','https://i.ibb.co/Y4qM7DHK/beijingroad.webp'),(7,'白云山风景区停车场','广州市白云区广园西中路801号','白云区',450,10,20.00,'open','06:00-22:00',113.273194,23.178466,'2025-11-18 21:18:13','2025-11-24 10:10:57','https://i.ibb.co/jkJQXMVL/baiyunmoutain.webp'),(8,'越秀公园停车场','广州市越秀区解放北路988号','越秀区',299,1,8.00,'open','06:00-22:00',113.2657,23.1435,'2025-11-18 22:01:32','2025-11-24 10:10:57','https://i.ibb.co/mVNMbzZw/yuexiupark.jpg'),(9,'广州动物园停车场','广州市越秀区先烈中路120号','越秀区',200,2,10.00,'open','08:00-18:00',113.3036,23.1483,'2025-11-18 22:01:32','2025-11-24 10:10:57','https://i.ibb.co/WWGXgdWg/zoo.jpg'),(10,'广东省博物馆停车场','广州市天河区珠江东路2号','天河区',400,2,6.00,'open','09:00-17:00',113.3263,23.1181,'2025-11-18 22:01:32','2025-11-24 10:10:57','https://i.ibb.co/kVQZS9qN/guangdongmuseum.jpg'),(11,'长隆欢乐世界停车场','广州市番禺区汉溪大道东299号','番禺区',1000,2,15.00,'open','09:00-23:00',113.3322,22.9968,'2025-11-18 22:01:32','2025-11-24 10:10:57','https://i.ibb.co/C3CzYcSM/changlong.webp'),(12,'百万葵园停车场(荔湾)','广州市荔湾区万顷沙镇百万葵园周边','荔湾区',399,2,3.00,'open','08:00-22:00',113.5678,22.6899,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/tMKmPbTd/baiwankuiyuan.webp'),(13,'正佳广场停车场(荔湾店)','广州市荔湾区天河路228号地下','荔湾区',130,3,15.00,'open','00:00-24:00',113.2322,23.1171,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/qYknn7wJ/zhengjia-square.jpg'),(14,'海鸥岛停车场(黄埔)','广州市黄埔区石楼镇海鸥岛周边','黄埔区',96,2,5.00,'open','07:00-21:00',113.5262,22.9554,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/HDFfJxtd/haiouisland.webp'),(15,'莲花山停车场(黄埔)','广州市黄埔区石楼镇莲花山周边','黄埔区',79,2,5.00,'open','06:00-22:00',113.4866,22.9751,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/TDRdY6kh/lianhuamoutain.jpg'),(16,'客村停车场','广州市番禺区艺苑路客村附近','番禺区',74,4,6.00,'open','00:00-24:00',113.3325,23.0991,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/3yjHJcLP/kecun.jpg'),(17,'祈福新村停车场(花都)','广州市花都区祈福大道祈福新村附近','花都区',77,2,8.00,'open','00:00-24:00',113.2088,23.3661,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/Cpy1LpH4/qifuxincun.webp'),(18,'天汇广场停车场(南沙)','广州市南沙区体育西路天汇广场地下','南沙区',23,2,16.00,'open','08:00-23:00',113.5683,22.7821,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/Pz5J3GYB/tianhui.webp'),(19,'广百百货停车场(增城)','广州市增城区北京路广百百货地下','增城区',63,2,16.00,'open','09:00-22:00',113.8266,23.2951,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/x86YZxmL/guangbaimall.webp'),(20,'丽江花园停车场(从化)','广州市从化区新园路丽江花园附近','从化区',165,2,7.00,'open','00:00-24:00',113.5855,23.5439,'2025-11-18 21:57:46','2025-11-24 10:10:57','https://i.ibb.co/b5L68cHq/lijiang.webp');
/*!40000 ALTER TABLE `parking_lot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parking_space`
--

DROP TABLE IF EXISTS `parking_space`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parking_space` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '车位ID',
  `parking_id` bigint NOT NULL COMMENT '所属停车场ID',
  `space_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '车位编号',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车位名称',
  `location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车位位置',
  `floor` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼层',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态 AVAILABLE/OCCUPIED/RESERVED',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'SMALL' COMMENT '类型 SMALL/MEDIUM/LARGE',
  `category` int DEFAULT '0' COMMENT '类别 0-普通 1-VIP 2-残疾人专用',
  `state` int DEFAULT '0' COMMENT '数字状态 0-空闲 1-锁定 2-占用',
  `hourly_rate` decimal(10,2) DEFAULT NULL COMMENT '小时费率',
  `daily_rate` decimal(10,2) DEFAULT NULL COMMENT '日费率',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车位图片',
  `is_available` int DEFAULT '1' COMMENT '是否可用 0-不可用 1-可用',
  `is_disabled` tinyint DEFAULT '0' COMMENT '是否禁用 0-正常 1-禁用',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_number` (`parking_id`,`space_number`),
  KEY `idx_parking_id` (`parking_id`),
  KEY `idx_status` (`status`),
  KEY `idx_state` (`state`),
  CONSTRAINT `fk_space_lot` FOREIGN KEY (`parking_id`) REFERENCES `parking_lot` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=195 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='停车位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parking_space`
--

LOCK TABLES `parking_space` WRITE;
/*!40000 ALTER TABLE `parking_space` DISABLE KEYS */;
INSERT INTO `parking_space` VALUES (115,1,'A1-01','A1区01号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,12.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(116,1,'A1-02','A1区02号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,12.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(117,1,'A1-03','A1区03号','A区1楼','L1','AVAILABLE','SMALL',0,0,12.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(118,1,'A1-04','A1区04号','A区1楼','L1','AVAILABLE','LARGE',0,0,15.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-20 14:23:42'),(119,1,'A2-01','A2区01号','A区2楼','L2','AVAILABLE','MEDIUM',0,0,12.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(120,1,'A2-02','A2区02号','A区2楼','L2','AVAILABLE','MEDIUM',0,0,12.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(121,1,'B1-01','B1区01号','B区1楼','L1','AVAILABLE','MEDIUM',1,0,15.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(122,1,'B1-02','B1区02号','B区1楼','L1','AVAILABLE','LARGE',1,0,18.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(123,1,'C1-01','C1区01号','C区1楼','L1','AVAILABLE','SMALL',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(124,1,'C1-02','C1区02号','C区1楼','L1','AVAILABLE','SMALL',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(125,2,'A1-01','A1区01号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-24 10:48:39'),(126,2,'A1-02','A1区02号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-21 22:20:43'),(127,2,'A1-03','A1区03号','A区1楼','L1','RESERVED','SMALL',0,1,10.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-24 13:57:33'),(128,2,'A2-01','A2区01号','A区2楼','L2','AVAILABLE','MEDIUM',0,0,10.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-21 16:40:54'),(129,2,'A2-02','A2区02号','A区2楼','L2','RESERVED','LARGE',0,1,12.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-23 16:36:41'),(130,2,'B1-01','B1区01号','B区1楼','L1','AVAILABLE','MEDIUM',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(131,2,'B1-02','B1区02号','B区1楼','L1','AVAILABLE','MEDIUM',1,0,12.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-23 16:36:02'),(132,2,'C1-01','C1区01号','C区1楼','L1','AVAILABLE','SMALL',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(133,2,'C1-02','C1区02号','C区1楼','L1','AVAILABLE','SMALL',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-23 16:31:40'),(134,2,'D1-01','D1区01号','D区1楼','L1','AVAILABLE','MEDIUM',2,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(135,3,'A1-01','A1区01号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,11.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-21 16:40:54'),(136,3,'A1-02','A1区02号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,11.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-24 12:38:30'),(137,3,'A2-01','A2区01号','A区2楼','L2','AVAILABLE','SMALL',0,0,11.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-19 20:14:15'),(138,3,'B1-01','B1区01号','B区1楼','L1','RESERVED','LARGE',0,1,13.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-23 16:32:19'),(139,3,'B1-02','B1区02号','B区1楼','L1','AVAILABLE','MEDIUM',1,0,13.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-19 20:14:15'),(140,3,'C1-01','C1区01号','C区1楼','L1','RESERVED','SMALL',0,1,9.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-22 11:25:14'),(141,4,'A1-01','A1区01号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-12-05 12:08:33'),(142,4,'A1-02','A1区02号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-22 13:46:57'),(143,4,'A2-01','A2区01号','A区2楼','L2','AVAILABLE','SMALL',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-24 11:39:25'),(144,4,'B1-01','B1区01号','B区1楼','L1','AVAILABLE','LARGE',0,0,12.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-24 13:57:05'),(145,4,'C1-01','C1区01号','C区1楼','L1','RESERVED','MEDIUM',1,1,12.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-21 22:20:51'),(146,5,'A1-01','A1区01号','A区1楼','L1','RESERVED','MEDIUM',0,1,8.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-22 13:54:30'),(147,5,'A1-02','A1区02号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-24 10:50:58'),(148,5,'A2-01','A2区01号','A区2楼','L2','RESERVED','SMALL',0,1,8.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-22 15:24:08'),(149,5,'B1-01','B1区01号','B区1楼','L1','RESERVED','LARGE',0,1,10.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-24 14:06:46'),(150,5,'C1-01','C1区01号','C区1楼','L1','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-24 11:46:47'),(151,6,'A1-01','A1区01号','A区1楼','L1','RESERVED','MEDIUM',0,1,8.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:18:13','2025-11-22 11:26:00'),(152,6,'A1-02','A1区02号','A区1楼','L1','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-23 14:02:18'),(153,6,'A2-01','A2区01号','A区2楼','L2','AVAILABLE','SMALL',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-23 16:17:29'),(154,6,'B1-01','B1区01号','B区1楼','L1','AVAILABLE','LARGE',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-23 16:17:46'),(155,6,'C1-01','C1区01号','C区1楼','L1','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-24 10:48:51'),(156,7,'A1-01','1楼A1区01号','1楼A1区','L1','AVAILABLE','MEDIUM',0,0,20.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(157,7,'A1-02','1楼A1区02号','1楼A1区','L1','AVAILABLE','MEDIUM',0,0,20.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(158,7,'A1-03','1楼A1区03号','1楼A1区','L1','AVAILABLE','LARGE',0,0,25.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(159,7,'A2-01','1楼A2区01号','1楼A2区','L1','AVAILABLE','SMALL',0,0,18.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(160,7,'A2-02','1楼A2区02号','1楼A2区','L1','AVAILABLE','MEDIUM',0,0,20.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(161,7,'B1-01','1楼B1区01号','1楼B1区','L1','AVAILABLE','MEDIUM',0,0,20.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(162,7,'B1-02','1楼B1区02号','1楼B1区','L1','AVAILABLE','LARGE',1,0,25.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(163,7,'C1-01','1楼C1区01号','1楼C1区','L1','AVAILABLE','SMALL',0,0,18.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(164,7,'C1-02','1楼C1区02号','1楼C1区','L1','AVAILABLE','MEDIUM',2,0,20.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(165,7,'D1-01','1楼D1区01号','1楼D1区','L1','AVAILABLE','MEDIUM',0,0,20.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:18:13','2025-11-18 21:18:13'),(166,12,'LW1-01','A区01号','A区','G','RESERVED','SMALL',0,1,3.00,NULL,NULL,NULL,0,0,0,'2025-11-18 21:57:46','2025-11-23 16:34:22'),(167,12,'LW1-02','A区02号','A区','G','AVAILABLE','SMALL',0,0,3.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(168,12,'LW1-03','A区03号','A区','G','AVAILABLE','LARGE',0,0,5.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-23 19:04:46'),(169,13,'LW2-A1','A区01','负1层A区','B1','AVAILABLE','MEDIUM',0,0,15.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(170,13,'LW2-A2','A区02','负1层A区','B1','AVAILABLE','MEDIUM',0,0,15.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(171,13,'LW2-B1','B区01','负2层B区','B2','AVAILABLE','SMALL',0,0,15.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(172,14,'HP1-01','地面01','入口区','G','AVAILABLE','LARGE',0,0,5.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(173,14,'HP1-02','地面02','入口区','G','AVAILABLE','LARGE',0,0,5.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(174,15,'HP2-01','A区01','A区','G','AVAILABLE','MEDIUM',0,0,5.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(175,15,'HP2-02','A区02','A区','G','AVAILABLE','MEDIUM',0,0,5.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(176,16,'PY1-01','B1-01','负1层','B1','AVAILABLE','SMALL',0,0,6.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(177,16,'PY1-02','B1-02','负1层','B1','AVAILABLE','SMALL',0,0,6.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(178,16,'PY1-03','B1-03','负1层','B1','AVAILABLE','MEDIUM',0,0,6.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-21 16:21:02'),(179,17,'HD4-01','A区01','A区','G','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(180,17,'HD4-02','A区02','A区','G','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(181,18,'NS1-A1','A区01','负1层','B1','AVAILABLE','SMALL',1,0,16.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(182,18,'NS1-A2','A区02','负1层','B1','AVAILABLE','SMALL',1,0,16.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(183,19,'ZC3-A1','A区01','负1层','B1','AVAILABLE','MEDIUM',0,0,16.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(184,19,'ZC3-A2','A区02','负1层','B1','AVAILABLE','MEDIUM',0,0,16.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(185,20,'CH4-01','A区01','A区','G','AVAILABLE','MEDIUM',0,0,7.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-18 21:57:46'),(186,20,'CH4-02','A区02','A区','G','AVAILABLE','MEDIUM',0,0,7.00,NULL,NULL,NULL,1,0,0,'2025-11-18 21:57:46','2025-11-23 14:45:53'),(187,8,'YX2-01','A区01','地面A区','G','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,1,0,0,'2025-11-18 22:01:32','2025-11-23 14:46:25'),(188,8,'YX2-02','A区02','地面A区','G','AVAILABLE','MEDIUM',0,0,8.00,NULL,NULL,NULL,0,0,0,'2025-11-18 22:01:32','2025-11-21 16:40:54'),(189,9,'ZW1-01','南门01','南门停车场','G','AVAILABLE','SMALL',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 22:01:32','2025-11-18 22:01:32'),(190,9,'ZW1-02','南门02','南门停车场','G','AVAILABLE','SMALL',0,0,10.00,NULL,NULL,NULL,1,0,0,'2025-11-18 22:01:32','2025-11-18 22:01:32'),(191,10,'BW1-01','负一01','地下负一层','B1','AVAILABLE','MEDIUM',0,0,6.00,NULL,NULL,NULL,0,0,0,'2025-11-18 22:01:32','2025-11-20 14:23:42'),(192,10,'BW1-02','负一02','地下负一层','B1','AVAILABLE','MEDIUM',0,0,6.00,NULL,NULL,NULL,1,0,0,'2025-11-18 22:01:32','2025-11-18 22:01:32'),(193,11,'CL1-01','北门01','北门停车场','G','AVAILABLE','LARGE',0,0,15.00,NULL,NULL,NULL,1,0,0,'2025-11-18 22:01:32','2025-11-18 22:01:32'),(194,11,'CL1-02','北门02','北门停车场','G','AVAILABLE','LARGE',0,0,15.00,NULL,NULL,NULL,1,0,0,'2025-11-18 22:01:32','2025-11-18 22:01:32');
/*!40000 ALTER TABLE `parking_space` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservation`
--

DROP TABLE IF EXISTS `reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `parking_space_id` bigint NOT NULL COMMENT '车位ID',
  `parking_id` bigint NOT NULL COMMENT '停车场ID',
  `reservation_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预约编号',
  `status` int DEFAULT '0' COMMENT '状态 0-待使用 1-已使用 2-已取消 3-已超时',
  `refund_status` int DEFAULT '0' COMMENT '退款状态 0-无退款 1-退款中 2-退款成功 3-退款失败',
  `start_time` datetime NOT NULL COMMENT '预约开始时间',
  `end_time` datetime NOT NULL COMMENT '预约结束时间',
  `actual_entry_time` datetime DEFAULT NULL COMMENT '实际入场时间',
  `actual_exit_time` datetime DEFAULT NULL COMMENT '实际出场时间',
  `plate_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '车牌号',
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `vehicle_info` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车辆信息',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注信息',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `payment_status` tinyint DEFAULT '0' COMMENT '支付状态：0-未支付，1-已支付',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parking_space_id` (`parking_space_id`),
  KEY `idx_parking_id` (`parking_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  CONSTRAINT `fk_res_lot` FOREIGN KEY (`parking_id`) REFERENCES `parking_lot` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_res_space` FOREIGN KEY (`parking_space_id`) REFERENCES `parking_space` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_res_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation`
--

LOCK TABLES `reservation` WRITE;
/*!40000 ALTER TABLE `reservation` DISABLE KEYS */;
INSERT INTO `reservation` VALUES (46,1,127,2,'RES20251124135733DA9D48',3,0,'2025-11-24 13:57:31','2025-11-24 15:57:31',NULL,NULL,'京A12345','13800138000','','',0,'2025-11-24 13:57:33','2025-12-05 09:22:15',0),(47,1,141,4,'RES20251124140515C9CC3A',1,0,'2025-11-24 14:05:14','2025-11-24 14:06:30','2025-11-24 14:05:20','2025-11-24 14:06:30','京A12345','13800138000','','',0,'2025-11-24 14:05:15','2025-11-24 14:06:33',1),(48,1,149,5,'RES2025112414064698E54B',3,0,'2025-11-24 14:06:47','2025-11-24 16:06:47',NULL,NULL,'粤A12345','13855158400',NULL,'语音预约：广州塔场',0,'2025-11-24 14:06:47','2025-12-05 09:22:15',0),(56,1,141,4,'RES20251205092215ABEA3A',2,0,'2025-12-05 09:22:15','2025-12-05 11:22:15',NULL,NULL,'粤A12345','13800138000','','',0,'2025-12-05 09:22:16','2025-12-05 12:08:33',0);
/*!40000 ALTER TABLE `reservation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_log`
--

DROP TABLE IF EXISTS `sys_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module` varchar(50) NOT NULL COMMENT '模块名称',
  `message` text COMMENT '错误简述',
  `content` text COMMENT '详细信息JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统异常日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_log`
--

LOCK TABLES `sys_log` WRITE;
/*!40000 ALTER TABLE `sys_log` DISABLE KEYS */;
INSERT INTO `sys_log` VALUES (1,'reservation','测试异常日志','{\"error\": \"Test Error\", \"details\": \"This is a mock log entry\"}','2025-12-05 08:16:32'),(2,'reservation','手动模拟的系统异常','{\"error\": \"Manual Test Error\", \"exception\": \"java.lang.RuntimeException: 测试日志\", \"user\": \"admin\"}','2025-12-05 09:34:08'),(3,'system','系统内存警告 (测试数据)','{\"memory_usage\": \"85%\", \"free_space\": \"512MB\", \"status\": \"warning\"}','2025-12-05 10:05:43'),(4,'system','数据库连接池警告','{\"active\": 90, \"idle\": 5}','2025-12-05 10:06:14');
/*!40000 ALTER TABLE `sys_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信OpenID',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `gender` int DEFAULT NULL COMMENT '性别 0-未知 1-男 2-女',
  `license_plate` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车牌号',
  `id_card_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号',
  `status` int DEFAULT '0' COMMENT '状态 0-正常 1-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_openid` (`openid`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'test_openid_001','微信用户','wxfile://tmp_cef54abdc4f159c7a71173a4fc358b199d23da27ce357584.jpg','13855158400',NULL,NULL,'粤A12345',NULL,0,'2025-11-19 15:45:09','2025-11-24 10:47:55');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-10 21:38:09
