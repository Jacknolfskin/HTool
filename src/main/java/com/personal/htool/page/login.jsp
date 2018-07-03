<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
<script src="<%=request.getContextPath()%>/static/bui/js/jquery-1.8.1.min.js"></script>
</head>
<body>
username: <input type="text" id="username"><br><br>  
password: <input type="password" id="password"><br><br>
<button id="loginbtn">登录</button>
</body>
<script type="text/javascript">
$('#loginbtn').click(function() {
	var param = {
		username : $("#username").val(),
		password : $("#password").val()
	};
	$.ajax({ 
		type: "post", 
		url: "<%=request.getContextPath()%>" + "/checkLogin.json", 
		data: param, 
		dataType: "json", 
		success: function(data) { 
			if(data.success == false){
				alert(data.errorMsg);
			}else{
				//登录成功
			    window.location.href = "<%=request.getContextPath()%>" +  "/loginsuccess.jhtml";
			}
		},
		error: function(data) { 
			alert("调用失败...."); 
		}
	});
});
</script>
</html>