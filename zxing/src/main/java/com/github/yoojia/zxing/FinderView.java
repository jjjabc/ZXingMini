package com.github.yoojia.zxing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.github.yoojia.qrcode.R;
import com.github.yoojia.zxing.camera.CameraManager;
import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class FinderView extends View {

	private static final long ANIMATION_DELAY = 10L;
	private static final int OPAQUE = 0xFF;

    private static final int MAX_RESULT_POINTS = 20;

    private static int MIDDLE_LINE_WIDTH;
    private static int MIDDLE_LINE_PADDING;
	private static final int SPEED_DISTANCE = 8;

	private Paint paint;
    private int CORNER_PADDING;
    private int slideTop;
	private int slideBottom;
    private boolean isFirst = true;

    private final int maskColor;
    private final int resultPointColor;
    private List<ResultPoint> possibleResultPoints;

	private List<ResultPoint> lastPossibleResultPoints;

	private CameraManager cameraManager;
    private Bitmap mCornerTopLeft;
    private Bitmap mCornerTopRight;
    private Bitmap mCornerBottomLeft;
    private Bitmap mCornerBottomRight;
    private Bitmap mScanLexer;

	// This constructor is used when the class is built from an XML resource.
	public FinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		CORNER_PADDING = dip2px(context, 0.0F);
		MIDDLE_LINE_PADDING = dip2px(context, 20.0F);
		MIDDLE_LINE_WIDTH = dip2px(context, 3.0F);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG); // 开启反锯齿
		final Resources resources = getResources();
		maskColor = 0xAA252525; // 遮掩层颜色
		resultPointColor = 0x88FF0000;
		possibleResultPoints = new ArrayList<>(5);
		lastPossibleResultPoints = null;

        mCornerTopLeft = BitmapFactory.decodeResource(resources, R.mipmap.scan_corner_top_left);
        mCornerTopRight = BitmapFactory.decodeResource(resources, R.mipmap.scan_corner_top_right);
        mCornerBottomLeft = BitmapFactory.decodeResource(resources, R.mipmap.scan_corner_bottom_left);
        mCornerBottomRight = BitmapFactory.decodeResource( resources, R.mipmap.scan_corner_bottom_right);
        mScanLexer = ((BitmapDrawable) getResources().getDrawable(R.mipmap.scan_laser)).getBitmap();
	}

	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (cameraManager == null) {
			return; // not ready yet, early draw before done configuring
		}
		Rect frame = cameraManager.getFramingRect();
		if (frame == null) {
			return;
		}
		// 绘制遮掩层
		drawCover(canvas, frame);
        // 画扫描框边上的角
        drawRectEdges(canvas, frame);
        // 绘制扫描线
        drawScanningLine(canvas, frame);
        List<ResultPoint> currentPossible = possibleResultPoints;
        Collection<ResultPoint> currentLast = lastPossibleResultPoints;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        }
        else {
            possibleResultPoints = new ArrayList<>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(OPAQUE);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentPossible) {
                canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
            }
        }
        if (currentLast != null) {
            paint.setAlpha(OPAQUE / 2);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentLast) {
                canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
            }
        }
        // 只刷新扫描框的内容，其他地方不刷新
        postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
	}

	/**
	 * 绘制扫描线
	 */
	private void drawScanningLine(Canvas canvas, Rect frame) {
		// 初始化中间线滑动的最上边和最下边
		if (isFirst) {
			isFirst = false;
			slideTop = frame.top;
			slideBottom = frame.bottom;
		}
		// 绘制中间的线,每次刷新界面，中间的线往下移动SPEED_DISTANCE
		slideTop += SPEED_DISTANCE;
		if (slideTop >= slideBottom) {
			slideTop = frame.top;
		}
		// 从图片资源画扫描线
		Rect lineRect = new Rect();
		lineRect.left = frame.left + MIDDLE_LINE_PADDING;
		lineRect.right = frame.right - MIDDLE_LINE_PADDING;
		lineRect.top = slideTop;
		lineRect.bottom = (slideTop + MIDDLE_LINE_WIDTH);
		canvas.drawBitmap(mScanLexer, null, lineRect, paint);

	}

	private void drawCover(Canvas canvas, Rect frame) {
		// 获取屏幕的宽和高
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(maskColor);
		// 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
		// 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);
	}

	/**
	 * 描绘方形的四个角
	 */
	private void drawRectEdges(Canvas canvas, Rect frame) {
		paint.setColor(Color.WHITE);
		paint.setAlpha(OPAQUE);
		canvas.drawBitmap(mCornerTopLeft, frame.left + CORNER_PADDING, frame.top + CORNER_PADDING, paint);
		canvas.drawBitmap(mCornerTopRight, frame.right - CORNER_PADDING - mCornerTopRight.getWidth(),
                frame.top + CORNER_PADDING, paint);
		canvas.drawBitmap(mCornerBottomLeft, frame.left + CORNER_PADDING,
                2 + (frame.bottom - CORNER_PADDING - mCornerBottomLeft.getHeight()), paint);
		canvas.drawBitmap(mCornerBottomRight, frame.right - CORNER_PADDING - mCornerBottomRight.getWidth(),
                2 + (frame.bottom - CORNER_PADDING - mCornerBottomRight.getHeight()), paint);
	}

	public void addPossibleResultPoint(final ResultPoint point) {
		List<ResultPoint> points = possibleResultPoints;
		synchronized (points) {
			points.add(point);
			int size = points.size();
			if (size > MAX_RESULT_POINTS) {
				// trim it
				points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
			}
		}
	}

	public int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

}
