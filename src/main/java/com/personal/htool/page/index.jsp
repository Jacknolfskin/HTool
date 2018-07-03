<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
<script src="<%=request.getContextPath()%>/static/bui/js/jquery-1.8.1.min.js"></script>
</head>
<body>
<button id="businessException">业务异常</button>&nbsp;&nbsp;
<button id="otherException">其他异常</button>
</body>
<script type="text/javascript">
$('#businessException').click(function() {
	$.ajax({ 
		type: "post", 
		url: "<%=request.getContextPath()%>" + "/businessException.json", 
		data: {}, 
		dataType: "json", 
		contentType : "application/json",
		success: function(data) { 
			if(data.success == false){
				alert(data.errorMsg);
			}else{
				alert("请求成功无异常"); 
			}
		},
		error: function(data) { 
			alert("调用失败...."); 
		}
	});
});

$('#otherException').click(function() {
	$.ajax({ 
		type: "post", 
		url: "<%=request.getContextPath()%>" + "/otherException.json", 
		data: {}, 
		dataType: "json", 
		contentType : "application/json",
		success: function(data) { 
			if(data.success == false){
				alert(data.errorMsg);
			}else{
				alert("请求成功无异常"); 
			}
		},
		error: function(data) { 
		    alert("调用失败...."); 
		}
	});
});

</script>
</html>