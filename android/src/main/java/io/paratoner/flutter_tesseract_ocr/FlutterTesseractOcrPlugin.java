package io.paratoner.flutter_tesseract_ocr;

import com.googlecode.tesseract.android.TessBaseAPI;

import androidx.annotation.NonNull;

import java.io.File;

import java.util.Map.*;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.os.Handler;
import android.os.Looper;
import android.os.AsyncTask;

public class FlutterTesseractOcrPlugin implements FlutterPlugin, MethodCallHandler {
  private static final int DEFAULT_PAGE_SEG_MODE = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
  TessBaseAPI baseApi = null;

  private MethodChannel channel;
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    // TODO: your plugin is now attached to a Flutter experience.
    BinaryMessenger messenger = flutterPluginBinding.getBinaryMessenger();
    channel = new MethodChannel(messenger, "flutter_tesseract_ocr");
    channel.setMethodCallHandler(this);

  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    // TODO: your plugin is no longer attached to a Flutter experience.
    channel.setMethodCallHandler(null);
    channel = null;
    if (this.baseApi != null){
      this.baseApi.recycle();
    }
    this.baseApi = null;

  }
  @Override
  public void onMethodCall(final MethodCall call, final Result result) {
    switch (call.method) {
      case "extractText":
      case "extractHocr":
        final String tessDataPath = call.argument("tessData");
        final String imagePath = call.argument("imagePath");
        final Map<String, String> args = call.argument("args");
        final String[] recognizedText = new String[1];
        String DEFAULT_LANGUAGE = "eng";
        if (call.argument("language") != null) {
          DEFAULT_LANGUAGE = call.argument("language");
        }
        calculateResult(recognizedText, tessDataPath, imagePath, DEFAULT_LANGUAGE, call.method.equals("extractHocr"), result);
        break;

      default:
        result.notImplemented();
    }
  }

  private void calculateResult(final String[] recognizedText, final String tessDataPath, final String imagePath, final String language, final Boolean isHocr,
      final Result result_) {
    new MyTask(recognizedText, tessDataPath, imagePath, language, isHocr, result_).execute();
  }

  private static class MyTask extends AsyncTask<Void, Void, Void> {
    String[] recognizedText;
    String tessDataPath;
    String imagePath;
    String language;
    Boolean isHocr;
    Result result_;

    MyTask(String[] recognizedText, String tessDataPath, String imagePath, String language, Boolean isHocr,
           Result result_) {
      this.recognizedText = recognizedText;
      this.tessDataPath = tessDataPath;
      this.imagePath = imagePath;
      this.language = language;
      this.isHocr = isHocr;
      this.result_ = result_;
    }

    @Override
    protected Void doInBackground(Void... params) {
      final TessBaseAPI baseApi = new TessBaseAPI();
      baseApi.init(this.tessDataPath, this.language);
      final File tempFile = new File(this.imagePath);
      baseApi.setPageSegMode(DEFAULT_PAGE_SEG_MODE);
      baseApi.setImage(tempFile);
      if (this.isHocr) {
        this.recognizedText[0] = baseApi.getHOCRText(0);
      } else {
        this.recognizedText[0] = baseApi.getUTF8Text();
      }
      baseApi.stop();
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      this.result_.success(this.recognizedText[0]);
    }
  }

}

