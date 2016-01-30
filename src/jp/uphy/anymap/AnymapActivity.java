package jp.uphy.anymap;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class AnymapActivity extends Activity {

  private static final String PREVIOUS_FILE = "prevfile";
  private ImageView imageView;
  private View star;
  private Button button;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    this.imageView = (ImageView)findViewById(R.id.imageView);
    this.star = findViewById(R.id.star);
    this.button = (Button)findViewById(R.id.button);

    this.imageView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(final View v, final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          final float x = event.getX();
          final float y = event.getY();

          star.setVisibility(View.VISIBLE);
          setAbsoluteLocationOfView(star, x, y);
          return true;
        }
        return false;
      }
    });
    this.button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        share(viewToImageFile((View)imageView.getParent()));
      }
    });

    Uri openedUri = openPreviousFile();
    if (openedUri == null) {
      openedUri = openIntentFile();
    }

    if (openedUri != null) {
      savePreviousFile(openedUri);
    }
  }

  private Uri openIntentFile() {
    final Intent intent = getIntent();
    Uri dataUri = intent.getData();
    if (dataUri == null) {
      final ClipData clipData = intent.getClipData();
      if (clipData == null) {
        toast("Open an image with this app.");
        finish();
        return null;
      }
      if (clipData.getItemCount() != 1) {
        toast("Specify one image to open.");
        return null;
      }
      dataUri = clipData.getItemAt(0).getUri();
    }

    showImage(dataUri);
    return dataUri;
  }

  private void share(Uri uriToImage) {
    final Intent shareIntent = new Intent();
    shareIntent.setAction(Intent.ACTION_SEND);
    shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
    shareIntent.setData(uriToImage);
    shareIntent.setType("image/png");
    startActivity(Intent.createChooser(shareIntent, "Share"));
  }

  private Uri viewToImageFile(View view) {
    final File anymapDir = new File(getExternalCacheDir(), "anymap");
    anymapDir.setReadable(true, false);
    if (anymapDir.exists() == false) {
      anymapDir.mkdirs();
    }
    final File tmpFile = new File(anymapDir, "location.png");
    tmpFile.setReadable(true, false);
    try (OutputStream out = new FileOutputStream(tmpFile)) {
      view.setDrawingCacheEnabled(false);
      view.setDrawingCacheEnabled(true);

      final Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    } catch (IOException e) {
      e.printStackTrace();
      toast("Failed to convert view to an image.");
    }
    return Uri.fromFile(tmpFile);
  }

  private void setAbsoluteLocationOfView(View view, float x, float y) {
    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
    int align = view.getMeasuredHeight() / 2;
    params.leftMargin = (int)x - align;
    params.topMargin = (int)y - align;
    view.setLayoutParams(params);
  }

  private void showImage(final Uri dataUri) {
    try (final InputStream stream = getContentResolver().openInputStream(dataUri)) {
      final Bitmap bitmap = BitmapFactory.decodeStream(stream);
      this.imageView.setImageBitmap(bitmap);
    } catch (IOException ex) {
      toast(ex.getMessage());
    }
  }

  private Uri openPreviousFile() {
    final String previousFile = getPreferences(MODE_PRIVATE).getString(PREVIOUS_FILE, null);
    if (previousFile == null) {
      return null;
    }
    try {
      final Uri previousUri = Uri.parse(previousFile);
      showImage(previousUri);
      return previousUri;
    } catch (Throwable e) {
      return null;
    }
  }

  private void savePreviousFile(final Uri dataUri) {
    try {
      final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
      final SharedPreferences.Editor editor = prefs.edit();
      editor.putString(PREVIOUS_FILE, dataUri.toString());
      editor.commit();
    } catch (Throwable e) {
      toast("Can not save the opened file: " + e.getMessage());
    }
  }

  private void toast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }
}
