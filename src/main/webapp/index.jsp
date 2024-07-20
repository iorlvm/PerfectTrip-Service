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
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
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
<form id="uploadForm" enctype="multipart/form-data">
    <label for="file">圖片上傳功能測試:</label>
    <input type="file" name="file" id="file" required>
    <br>
    <label for="comment">備註:
        <input type="text" name="comment" id="comment">
    </label>
    <br>
    <div>圖片緩存機制:
        <div style="display: flex; gap: 10px">
            <label><input type="radio" name="cacheEnabled" value="true"> 開啟</label>
            <label><input type="radio" name="cacheEnabled" value="false"> 關閉</label>
            <label><input type="radio" name="cacheEnabled" value="" checked> 自動</label>
        </div>
    </div>
    <br>
    <div>圖片壓縮機制:
        <div style="display: flex; gap: 10px">
            <label><input type="radio" name="resizeEnabled" value="true">開啟</label>
            <label><input type="radio" name="resizeEnabled" value="false">關閉</label>
            <label><input type="radio" name="resizeEnabled" value="" checked>自動</label>
        </div>
    </div>
    <label for="width">寬度:
        <input type="number" name="width" id="width" min="1">
    </label>
    <br>
    <label for="height">高度:
        <input type="number" name="height" id="height" min="1">
    </label>
    <br>
    <br>
    <input type="submit" value="上傳圖片">
</form>
<div style="width: 300px">
    <h3>圖片顯示測試</h3>
    <img id="uploadedImage" src="" alt="Uploaded Image" style="max-width: 100%;">
</div>

<script>
    document.getElementById('uploadForm').addEventListener('submit', async function(event) {
        event.preventDefault(); // 防止表單提交

        const form = event.target;
        const formData = new FormData(form);

        try {
            const response = await fetch('/image', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error('上傳失敗');
            }

            const result = await response.json();
            if (result.success) {
                document.getElementById('uploadedImage').src = result.data;
            } else {
                alert(result.errorMsg || '上傳失敗');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('上傳失敗：系統錯誤');
        }
    });
</script>
</body>
</html>
