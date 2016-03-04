package downloaddata.com.feeds.asynctask;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import downloaddata.com.feeds.R;

/**
 * Asynch Task To download Images from the Url
 */
public class BitMapWorkerTask extends AsyncTask<String, Void, Bitmap> {

    static String sData;
    public LruCache<String, Bitmap> mMemoryCache;


    // Use 1/8th of the available memory for this memory cache.
    int mCacheSize;
    private Context mContext;
    int memClass;
    private final WeakReference<ImageView> mImageViewReference;


    public BitMapWorkerTask(Context context, ImageView imageView) {
        mImageViewReference = new WeakReference<ImageView>(imageView);
        memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        mCacheSize = 1024 * 1024 * memClass / 8;
        mMemoryCache = new LruCache<String, Bitmap>(mCacheSize) {

            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number of items.
                return bitmap.getByteCount();
            }

        };  //this.path=imageView.getTag().toString();
    }

    public BitMapWorkerTask(ImageView view) {
        mImageViewReference = new WeakReference<ImageView>(view);
    }

    @Override
    // Actual download method, run in the task thread
    protected Bitmap doInBackground(String... params) {
        // params comes from the execute() call: params[0] is the url.
        sData = params[0];
        Bitmap bitmap = downloadBitmap(params[0]);
        if (bitmap != null) {
            addBitmapToMemoryCache(params[0], bitmap);
        }
        return bitmap;
    }

    @Override
    // Once the image is downloaded, associates it to the imageView
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }

        if (mImageViewReference != null && bitmap != null) {
            final ImageView imageView = mImageViewReference.get();
            final BitMapWorkerTask bitmapWorkerTask =
                    getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
            if (getBitmapFromMemCache(sData) != null)
                imageView.setImageBitmap(getBitmapFromMemCache(sData));
        }
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return (Bitmap) mMemoryCache.get(key);
    }

    public void loadBitmap(Context context, String path, ImageView imageView) {

        mContext = context;
        Bitmap placeholder = BitmapFactory.decodeResource(context.getResources(),R.drawable.placeholder);
        if (cancelPotentialWork(path, imageView)) {
            BitMapWorkerTask bitMapWorkerTask = new BitMapWorkerTask(context, imageView);


            final AysncDrawable aysncDrawable = new AysncDrawable(context.getResources(), bitMapWorkerTask, placeholder);

            imageView.setImageDrawable(aysncDrawable);
            bitMapWorkerTask.execute(path);

        }
    }

    public static boolean cancelPotentialWork(String path, ImageView imageView) {
        final BitMapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = sData;
            if (bitmapData != path) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitMapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AysncDrawable) {
                final AysncDrawable asyncDrawable = (AysncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public class AysncDrawable extends BitmapDrawable {
        private final WeakReference<BitMapWorkerTask> bitMapWorkerTaskReference;

        AysncDrawable(Resources r, BitMapWorkerTask bitMapWorkerTask, Bitmap bitmap) {
            super(r, bitmap);
            bitMapWorkerTaskReference = new WeakReference<BitMapWorkerTask>(bitMapWorkerTask);

        }

        public BitMapWorkerTask getBitmapWorkerTask() {
            return bitMapWorkerTaskReference.get();
        }
    }

    private Bitmap downloadBitmap(String url) {
        HttpURLConnection urlConnection = null;
        try {
            URL uri = new URL(url);
            urlConnection = (HttpURLConnection) uri.openConnection();
            int statusCode = urlConnection.getResponseCode();
            if (statusCode != 200) {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                return bitmap;
            }
        } catch (Exception e) {
            // urlConnection.disconnect();
            Log.w("ImageDownloader", "Error downloading image from " + url);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }
}


