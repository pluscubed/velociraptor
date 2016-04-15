package com.pluscubed.velociraptor.appselection;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.pluscubed.velociraptor.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AppIconLoader implements ModelLoader<AppInfo, InputStream> {

    final Context mContext;
    boolean mCancelled;

    public AppIconLoader(Context context) {
        mContext = context;
    }

    InputStream drawableToStream(Drawable d) {
        Bitmap bitmap;
        if (d instanceof BitmapDrawable) {
            BitmapDrawable bitDw = (BitmapDrawable) d;
            bitmap = bitDw.getBitmap();
        } else {
            int width = mContext.getResources().getDimensionPixelSize(R.dimen.icon_size);
            //noinspection SuspiciousNameCombination
            bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            d.draw(canvas);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);

        byte[] imageInByte = stream.toByteArray();
        InputStream is = new ByteArrayInputStream(imageInByte);

        if (mCancelled)
            return null;
        return is;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(final AppInfo model, int width, int height) {
        return new DataFetcher<InputStream>() {
            @Override
            public InputStream loadData(Priority priority) throws Exception {
                mCancelled = false;
                PackageManager pm = mContext.getPackageManager();
                if (mCancelled) return null;
                return drawableToStream(pm.getApplicationInfo(model.packageName, 0).loadIcon(pm));
            }

            @Override
            public void cleanup() {
            }

            @Override
            public String getId() {
                return String.valueOf(model.packageName.hashCode());
            }

            @Override
            public void cancel() {
                mCancelled = true;
            }
        };
    }

    public static class Factory implements ModelLoaderFactory<AppInfo, InputStream> {

        @Override
        public ModelLoader<AppInfo, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new AppIconLoader(context);
        }

        @Override
        public void teardown() {
        }
    }
}