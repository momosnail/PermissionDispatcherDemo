package com.base.permissiondispatcherdemo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    private static final int CODE_REQUEST_CAMERA_PERMISSIONS = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        findViewById (R.id.tv).setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                MainActivityPermissionsDispatcher.myNCamWithCheck (MainActivity.this);
            }
        });

    }

    @NeedsPermission(Manifest.permission.CAMERA)//允许请求权限后，后面要执行的逻辑写在这里
    void myNCam() {
        Toast.makeText (this,"权限被允许，本逻辑可以执行了",Toast.LENGTH_LONG).show ();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult (requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult (this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.CAMERA)//这里可以弹出一个对话框让用户点击是否要允许该权限，如果点击允许，则弹出权限对话框让用户选择
    void mySCam(final PermissionRequest request) {
        showRationaleDialog (R.string.permission_camera_rationale, request);
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)//如果用户没有选择:不再询问，且拒绝了该权限，则会回调此注解的方法
    void myDCam() {
        Toast.makeText (this, R.string.permission_camera_denied, Toast.LENGTH_LONG).show ();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)//如果用户选择让设备“不再询问”一个权限，则调用该方法
    void myACam() {
        Toast.makeText (this, R.string.permission_camera_never_askagain, Toast.LENGTH_SHORT).show ();
        //对话框提示语句仿支付宝提示
        final AlertDialog.Builder mDialog = new AlertDialog.Builder (this);
        mDialog.setTitle ("获取相机权限");
        mDialog.setMessage ("我们需要获取相机相关权限，用于拍照；否则，你将无法正常使用本应用\n设置路径：设置->应用->xxx->权限");
        mDialog.setNegativeButton ("取消", new DialogInterface.OnClickListener () {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDialog.create ().dismiss ();

            }
        });
        mDialog.setPositiveButton ("去设置", new DialogInterface.OnClickListener () {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                settingPermissionActivity (MainActivity.this);
            }
        });

        mDialog.show ();
    }

    private void showRationaleDialog(@StringRes int messageResId, final PermissionRequest request) {
        new AlertDialog.Builder (this)
                .setPositiveButton (R.string.button_allow, new DialogInterface.OnClickListener () {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed ();
                    }
                })
                .setNegativeButton (R.string.button_deny, new DialogInterface.OnClickListener () {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel ();
                    }
                })
                .setCancelable (false)
                .setMessage (messageResId)
                .show ();
    }

    public static void settingPermissionActivity(Activity activity) {
        //判断是否为小米系统
        if (TextUtils.equals (BrandUtils.getSystemInfo ().getOs (), BrandUtils.SYS_MIUI)) {
            Intent miuiIntent = new Intent ("miui.intent.action.APP_PERM_EDITOR");
            miuiIntent.putExtra ("extra_pkgname", activity.getPackageName ());
            //检测是否有能接受该Intent的Activity存在
            List<ResolveInfo> resolveInfos = activity.getPackageManager ().queryIntentActivities (miuiIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfos.size () > 0) {
                activity.startActivityForResult (miuiIntent, CODE_REQUEST_CAMERA_PERMISSIONS);
                return;
            }
        }
        //如果不是小米系统 则打开Android系统的应用设置页
        Intent intent = new Intent (Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts ("package", activity.getPackageName (), null);
        intent.setData (uri);
        activity.startActivityForResult (intent, CODE_REQUEST_CAMERA_PERMISSIONS);
    }
}
