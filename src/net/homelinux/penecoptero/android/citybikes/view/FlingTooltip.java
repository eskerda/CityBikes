package net.homelinux.penecoptero.android.citybikes.view;

import net.homelinux.penecoptero.android.citybikes.utils.CircleHelper;
import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class FlingTooltip extends View {
	private Paint rightPaint;
	private Paint leftPaint;
	private int height, width;
	private float scale;
	
	private static final float LINE_WIDTH = 3;
	private static final float MARGINS = 50;
	private static final float ARROW_WIDTH = 20;
	private static final int DIRECTION_RIGHT = 0;
	private static final int DIRECTION_LEFT = 1;
	
	private int line_width;
	private int margins;
	private int arrow_width;
	
	private int direction;
	
	public FlingTooltip(Context context) {
		super(context);
		init(context);
		// TODO Auto-generated constructor stub
	}

	public FlingTooltip(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
		// TODO Auto-generated constructor stub
	}
	
	public void setDirection (int direction){
		this.direction = direction;
	}
	
	private void init(Context context){
		height = getMeasuredHeight();
		width = getMeasuredWidth();
		scale = context.getResources().getDisplayMetrics().density;
		
		line_width = CircleHelper.dip2px(LINE_WIDTH, scale);
		margins = CircleHelper.dip2px(MARGINS, scale);
		arrow_width = CircleHelper.dip2px(ARROW_WIDTH, scale);
		
		rightPaint = new Paint();
		rightPaint.setStyle(Style.FILL);
		rightPaint.setStrokeWidth(1);
		rightPaint.setAntiAlias(true);
		rightPaint.setARGB(180, 191, 210, 85);
		int startRight= Color.rgb(191, 210, 85);
		int endRight = Color.rgb(147, 186, 43);
		Shader rightShader = new LinearGradient(0, 0, width, arrow_width* (float) 0.835, startRight, endRight, Shader.TileMode.REPEAT);
		//rightPaint.setShader(rightShader);
		
		leftPaint = new Paint();
		leftPaint.setStyle(Style.FILL);
		leftPaint.setStrokeWidth(1);
		leftPaint.setAntiAlias(true);
		leftPaint.setARGB(180, 147, 186, 228);
		int startLeft = Color.rgb(147, 186, 228);
		int endLeft = Color.rgb( 95, 121, 150);
		Shader leftShader = new LinearGradient(0, 0, width, arrow_width * (float) 0.835 , startLeft, endLeft, Shader.TileMode.REPEAT);
		//leftPaint.setShader(leftShader);
		
		
	}

	@Override
    protected void dispatchDraw(Canvas canvas) {
		Paint paint;
		if (direction == DIRECTION_RIGHT){
			paint = rightPaint;
		} else {
			paint = rightPaint;
		}
		canvas.drawPath(
				createArrow(
						new Point(margins,height/2),
						new Point(width-margins, height/2), 
						arrow_width, line_width, direction), paint);
		super.dispatchDraw(canvas);
	}
	
	public static Path createArrow(Point left, Point right, int width, int line_width, int direction)
	{
		Path path = new Path();
		if (direction == DIRECTION_LEFT){
			Point tmp = right;
			right = left;
			left = tmp;
		}else{
			
		}
		float radius = (float) (width * 0.45);
		double angle = Math.atan2(right.y - left.y, right.x - left.x);
		path.setFillType(Path.FillType.WINDING);
		RectF oval = new RectF(left.x-radius, left.y - radius, left.x + radius, left.y + radius);
		path.addOval(oval, Path.Direction.CCW);
		if (direction == DIRECTION_LEFT){
			RectF line = new RectF(left.x, left.y - line_width, right.x+width/2, right.y + line_width);
			path.addRect(line, Path.Direction.CW);
		}else{
			RectF line = new RectF(left.x, left.y - line_width, right.x-width/2, right.y + line_width);
			path.addRect(line, Path.Direction.CCW);
		}
			
		path.moveTo(left.x, left.y);
		path.moveTo(right.x, right.y);
		path.lineTo(right.x - width * (float) Math.cos(angle - Math.PI / 6) ,right.y - width * (float) Math.sin(angle - Math.PI / 6) );
		path.moveTo(right.x, right.y);
		path.lineTo(right.x - width * (float) Math.cos(angle + Math.PI / 6) ,right.y - width * (float) Math.sin(angle + Math.PI / 6) );
		path.lineTo(right.x - width * (float) Math.cos(angle - Math.PI / 6) ,right.y - width * (float) Math.sin(angle - Math.PI / 6) );
		
		return path;
		
	}
	
	 @Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			// TODO Auto-generated method stub
			super.onSizeChanged(w, h, oldw, oldh);
			height = h;
			width = w;
		}
}
