package com.example.tyhj.tvshow;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.tyhj.tvshow.service.MyService;
import com.example.tyhj.tvshow.service.MyService_;
import com.example.tyhj.tvshow.utils.BitmapUtils;
import com.example.tyhj.tvshow.utils.Connect;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity{

    int i=0;
    private Camera camera;
    private SurfaceHolder mSurfaceHolder;
    private boolean what=false;
    private long time=0;
    boolean first=true;
    PowerManager.WakeLock mWakeLock;


    @ViewById
    SurfaceView mSurfaceView;

    @ViewById
    ImageButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this,MyService_.class));
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
    }


    @Click(R.id.fab)
    void fab(){
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(success){
                    camera.takePicture(null,null,JpgmPicture);
                }
            }
        });
    }

    @Click(R.id.mSurfaceView)
    void change(){
        if(System.currentTimeMillis()-time<1000){
            what=!what;
            initCamera(what);
        }else {
            time=System.currentTimeMillis();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                }
            });
        }
    }

    @AfterViews
    void  afterView(){
        mSurfaceHolder=mSurfaceView.getHolder();
        agin();
    }

    @Background
    void agin(){
        try {
            initCamera(what);
            Thread.sleep(200);
            initCamera(what);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    void initCamera(boolean what){
        try {
            int position=0;
            int rotate=0;
            if(what) {
                position = 1;
                rotate=180;
            }
            destroyCamera();
            camera=Camera.open(position);
            camera.setPreviewDisplay(mSurfaceHolder);
            Camera.Parameters parameter=camera.getParameters();
            parameter.setPreviewFormat(ImageFormat.NV21);

            List<Camera.Size> sizeList=parameter.getSupportedPreviewSizes();
            for(int i=0;i<sizeList.size();i++){
                Log.e("支持的尺寸大小","x："+sizeList.get(i).width+" y"+sizeList.get(i).height);
            }

            parameter.setPreviewSize(720,480);


            //设置预览方向
            camera.setDisplayOrientation(90);
            //设置拍照之后图片方向
            parameter.setRotation(rotate);
            camera.setParameters(parameter);
            camera.setPreviewCallback(back);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //处理每一帧
    Camera.PreviewCallback  back=new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            if(i/30!=0){
                return;
            }

            Camera.Size size=camera.getParameters().getPreviewSize();
            YuvImage image=new YuvImage(data,ImageFormat.NV21,size.width,size.height,null);
            Bitmap bitmap=null;
            String string=null;
            if(image!=null){
                try {

                    ByteArrayOutputStream stream=new ByteArrayOutputStream();
                    image.compressToJpeg(new Rect(0,0,size.width,size.height),20,stream);
                    //bitmap= BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size());
                    //加水印
                    //bitmap= BitmapUtils.addLogo(MainActivity.this,bitmap,"小萨专用",R.drawable.watermark);

                    byte[] bytes = stream.toByteArray();

                    string = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    string.replace("\n","小萨");
                    sendMsg(string);
                    stream.flush();
                    stream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //拍照的图片
    private Camera.PictureCallback JpgmPicture=new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File file=getOutputMediaFile();
            if(file==null)
                return;
            try {
                FileOutputStream fos=new FileOutputStream(file);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
        }
    };

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
        return mediaFile;
    }

    private void destroyCamera(){
        if(camera!=null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCamera();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initCamera(what);
    }


    @Background
    void sendMsg(String bitmap){
        try {
            Connect.getInstance(MyService.IP, 9890, null).sendMsg(bitmap);
            //Connect.getInstance(MyService.IP, 9890, null).sendMsg(bitmaptoString(bitmap));
        }catch (Exception e){
            if(first){
                Snackbar.make(fab,"连接异常",Snackbar.LENGTH_SHORT).show();
                first=false;
            }
        }
    }

    public synchronized String bitmaptoString(Bitmap bitmap) {
        // 将Bitmap转换成字符串
        String string = null;
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, bStream);
            bStream.flush();
            bStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (bStream != null) {
                    bStream.flush();
                    bStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] bytes = bStream.toByteArray();
        string = Base64.encodeToString(bytes, Base64.NO_WRAP);
        string.replace("\n","小萨");
        return string;
    }


    //计算图片的缩放值
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }


    @Override
    protected void onResume() {
        super.onResume();
        mWakeLock.acquire();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mWakeLock.release();
    }
}
