DROP PROCEDURE IF EXISTS pr_add;

/*简单入门*/
CREATE PROCEDURE pr_add(
  a int
  )

BEGIN
  SELECT * FROM log l WHERE l.id=a;
  END;

  SET @a = 2;
  CALL pr_add(@a);
  

  /*存储过程循环*/
CREATE PROCEDURE pr_add(
  IN type int
  )
  BEGIN
  DECLARE c int;
  DECLARE b int;
  SET c = 0;
  SET b = 0;
  WHILE c <= type DO
  SET b=b+c;
  SET c=c+1;
  END WHILE;
SELECT b AS '选择' ;
END;

SET @type = 100;
CALL pr_add(@type);