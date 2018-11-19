package com.personal.htool.sql;

import com.personal.htool.generateid.IdGen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @Auther: ifly
 * @Date: 2018/11/19 08:35
 * @Description: 转：https://www.itcodemonkey.com/article/11255.html
 * 8秒插入1000万条数据到MySQL数据库表
 */
public class InsertTest {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        final String url = "jdbc:mysql://127.0.0.1/teacher";
        final String name = "com.mysql.jdbc.Driver";
        final String user = "root";
        final String password = "123456";
        Connection conn = null;
        Class.forName(name);
        conn = DriverManager.getConnection(url, user, password);
        if (conn != null) {
            System.out.println("获取连接成功");
            insert(conn);
        } else {
            System.out.println("获取连接失败");
        }

    }

    public static void insert(Connection conn) {
        Long begin = System.currentTimeMillis();
        String prefix = "INSERT INTO t_teacher (id,t_name,t_password,sex,description,pic_url,school_name,regist_date,remark) VALUES ";
        try {
            // 设置事务为非自动提交
            StringBuffer suffix = new StringBuffer();
            conn.setAutoCommit(false);
            // 比起st，pst会更好些
            PreparedStatement pst = (PreparedStatement) conn.prepareStatement("");
            // 外层循环，总提交事务次数
            for (int i = 1; i <= 100; i++) {
                // 第j次提交步长
                suffix = new StringBuffer();
                for (int j = 1; j <= 100000; j++) {
                    suffix.append("('" + IdGen.uuid() + "','" + i * j + "','123456'" + ",'男'" + ",'教师'" + ",'www.bbk.com'" + ",'XX大学'" + ",'" + "2016-08-12 14:43:26" + "','备注'" + "),");
                }
                String sql = prefix + suffix.substring(0, suffix.length() - 1);
                pst.addBatch(sql);
                pst.executeBatch();//提交事务
                conn.commit();//清空上一次添加的数据
                suffix = new StringBuffer();
            }
            pst.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Long end = System.currentTimeMillis();
        System.out.println("1000万条数据插入花费时间 : " + (end - begin) / 1000 + " s");
        System.out.println("插入完成");
    }
}
