<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>簡易表單功能測試頁</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f4f4f4;
            margin: 20px;
            padding: 0;
            height: 100vh;
            display: flex;
            align-items: flex-start;
            flex-wrap: wrap;
        }
        form {
            background-color: #fff;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
            width: 300px;
        }
        label {
            display: block;
            margin-top: 10px;
            color: #333;
        }
        input[type="text"], input[type="file"], input[type="radio"], input[type="submit"] {
            display: block;
            margin-top: 5px;
            width: 100%;
            padding: 8px;
            box-sizing: border-box;
        }
        input[type="radio"] {
            display: inline-block;
            width: auto;
            margin-right: 10px;
        }
        input[type="submit"] {
            background-color: #e26237;
            color: #fff;
            border: none;
            cursor: pointer;
            margin-top: 20px;
        }
        input[type="submit"]:hover {
            background-color: #d15130;
        }
    </style>
</head>
<body>
<form action="image" method="post" enctype="multipart/form-data">
    <label for="file">圖片上傳功能測試:</label>
    <input type="file" name="file" id="file">
    <br>
    <label for="comment">comment:
        <input type="text" name="comment" id="comment">
    </label>
    <br>
    <label for="cache">圖片緩存機制:<br>
        on <input type="radio" name="cache" value="y">
        off <input type="radio" name="cache" value="n">
        auto <input type="radio" name="cache" value="auto" checked>
    </label>
    <br>
    <br>
    <input type="submit" value="Upload Image" name="submit">
</form>

</body>
</html>
