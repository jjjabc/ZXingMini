# ZXingMini ZXing开源库的精简版

基于ZXing Android实现的二维码扫描支持库。
包括`生成二维码图片`和`相机扫描二维码图片即时解码`两部分功能。

## 本项目作以下优化：

1. 已创建QRCode Library，可以直接通过dependencies引入依赖使用。
2. 精简ZXing官方项目的例子，app模块为完整使用演示代码；


## 生成二维码图片

ZXingMini已对ZXing生成二维码图做了封装，通过 QRCodeEncode 类可以将文本内容生成二维码图片。

```java

    final int dimension = 500;
    final QRCodeEncode encoder = new QRCodeEncode.Builder()
                    .setBackgroundColor(0xFFFFFF) // 指定背景颜色，默认为白色
                    .setCodeColor(0xFF000000) // 指定编码块颜色，默认为黑色
                    .setOutputBitmapWidth(dimension) // 生成图片宽度
                    .setOutputBitmapHeight(dimension) // 生成图片高度
                    .build();

    final Bitmap _QRCodeImage = encoder.encode("你的文本内容");

```

## 对二维码图片解码

ZXingMini已对ZXing二维码解码部分做了封装。通过 QRCodeDecode 类可以将二维码图片解码为文本内容。

```java

    Bitmap _QRCodeImage = ....
    QRCodeDecode decoder = new QRCodeDecode.Builder().build();
    String content = decoder.decod(_QRCodeImage);

```

通常，二维码解码与相机联合使用。ZXingMini对相机管理的代码也做了简化，使用起来非常简单。详细代码参考QRCodeScanActivity代码。