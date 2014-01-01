package org.yanzi.testfacedetect;

import org.yanzi.util.ImageUtil;
import org.yanzi.util.MyToast;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
	static final String tag = "yan";
	ImageView imgView = null;
	FaceDetector faceDetector = null;
	FaceDetector.Face[] face;
	Button detectFaceBtn = null;
	final int N_MAX = 2;
	ProgressBar progressBar = null;

	Bitmap srcImg = null;
	Bitmap srcFace = null;
	Thread checkFaceThread = new Thread(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Bitmap faceBitmap = detectFace();
			mainHandler.sendEmptyMessage(2);
			Message m = new Message();
			m.what = 0;
			m.obj = faceBitmap;
			mainHandler.sendMessage(m);

		}

	};
	Handler mainHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//super.handleMessage(msg);
			switch (msg.what){
			case 0:
				Bitmap b = (Bitmap) msg.obj;
				imgView.setImageBitmap(b);
				MyToast.showToast(getApplicationContext(), "检测完毕");
				break;
			case 1:
				showProcessBar();
				break;
			case 2:
				progressBar.setVisibility(View.GONE);
				detectFaceBtn.setClickable(false);
				break;
			default:
				break;
			}
		}

	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initUI(); 
		initFaceDetect();
		detectFaceBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mainHandler.sendEmptyMessage(1);
				checkFaceThread.start();

			}
		});



	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	public void initUI(){

		detectFaceBtn = (Button)findViewById(R.id.btn_detect_face);
		imgView = (ImageView)findViewById(R.id.imgview);
		LayoutParams params = imgView.getLayoutParams();
		DisplayMetrics dm = getResources().getDisplayMetrics();
		int w_screen = dm.widthPixels;
		//		int h = dm.heightPixels;

		srcImg = BitmapFactory.decodeResource(getResources(), R.drawable.kunlong);
		int h = srcImg.getHeight();
		int w = srcImg.getWidth();
		float r = (float)h/(float)w;
		params.width = w_screen;
		params.height = (int)(params.width * r);
		imgView.setLayoutParams(params);
		imgView.setImageBitmap(srcImg);
	}

	public void initFaceDetect(){
		this.srcFace = srcImg.copy(Config.RGB_565, true);
		int w = srcFace.getWidth();
		int h = srcFace.getHeight();
		Log.i(tag, "待检测图像: w = " + w + "h = " + h);
		faceDetector = new FaceDetector(w, h, N_MAX);
		face = new FaceDetector.Face[N_MAX];
	}
	public boolean checkFace(Rect rect){
		int w = rect.width();
		int h = rect.height();
		int s = w*h;
		Log.i(tag, "人脸 宽w = " + w + "高h = " + h + "人脸面积 s = " + s);
		if(s < 10000){
			Log.i(tag, "无效人脸，舍弃.");
			return false;
		}
		else{
			Log.i(tag, "有效人脸，保存.");
			return true;	
		}
	}
	public Bitmap detectFace(){
		//		Drawable d = getResources().getDrawable(R.drawable.face_2);
		//		Log.i(tag, "Drawable尺寸 w = " + d.getIntrinsicWidth() + "h = " + d.getIntrinsicHeight());
		//		BitmapDrawable bd = (BitmapDrawable)d;
		//		Bitmap srcFace = bd.getBitmap();

		int nFace = faceDetector.findFaces(srcFace, face);
		Log.i(tag, "检测到人脸：n = " + nFace);
		for(int i=0; i<nFace; i++){
			Face f  = face[i];
			PointF midPoint = new PointF();
			float dis = f.eyesDistance();
			f.getMidPoint(midPoint);
			int dd = (int)(dis);
			Point eyeLeft = new Point((int)(midPoint.x - dis/2), (int)midPoint.y);
			Point eyeRight = new Point((int)(midPoint.x + dis/2), (int)midPoint.y);
			Rect faceRect = new Rect((int)(midPoint.x - dd), (int)(midPoint.y - dd), (int)(midPoint.x + dd), (int)(midPoint.y + dd));
			Log.i(tag, "左眼坐标 x = " + eyeLeft.x + "y = " + eyeLeft.y);
			if(checkFace(faceRect)){
				Canvas canvas = new Canvas(srcFace);
				Paint p = new Paint();
				p.setAntiAlias(true);
				p.setStrokeWidth(8);
				p.setStyle(Paint.Style.STROKE);
				p.setColor(Color.GREEN);
				canvas.drawCircle(eyeLeft.x, eyeLeft.y, 20, p);
				canvas.drawCircle(eyeRight.x, eyeRight.y, 20, p);
				canvas.drawRect(faceRect, p);
			}

		}
		ImageUtil.saveJpeg(srcFace);
		Log.i(tag, "保存完毕");

		//将绘制完成后的faceBitmap返回
		return srcFace;

	}
	public void showProcessBar(){
		RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.layout_main);
		progressBar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleLargeInverse); //ViewGroup.LayoutParams.WRAP_CONTENT
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		progressBar.setVisibility(View.VISIBLE);
		//progressBar.setLayoutParams(params);
		mainLayout.addView(progressBar, params);

	}


}
