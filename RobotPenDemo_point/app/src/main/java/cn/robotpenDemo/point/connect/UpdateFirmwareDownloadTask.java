package cn.robotpenDemo.point.connect;

import android.os.AsyncTask;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class UpdateFirmwareDownloadTask extends AsyncTask<String, Integer, List<byte[]>> {

    @Override
    protected List<byte[]> doInBackground(String... params) {
        List<byte[]> result = new ArrayList<>();
        for (int i = 0; i < params.length; i++) {
            String url = params[i];
            if (!TextUtils.isEmpty(url)) {
                result.add(down(i, url));
            }
        }
        return result;
    }

    private byte[] down(int index, String u) {
        try {
            URL url = new URL(u);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(false);
            urlConnection.setConnectTimeout(10 * 1000);
            urlConnection.setReadTimeout(10 * 1000);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Charset", "UTF-8");
            urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            urlConnection.connect();
            InputStream in = urlConnection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int bytetotal = urlConnection.getContentLength();
            int bytesum = 0;
            int byteread;
            byte[] buffer = new byte[1024];
            while ((byteread = in.read(buffer)) != -1 && !isCancelled()) {
                bytesum += byteread;
                outputStream.write(buffer, 0, byteread);
                publishProgress(bytesum, bytetotal, index);
            }
            outputStream.flush();
            outputStream.close();
            in.close();
            byte[] result = outputStream.toByteArray();
//            outputStream.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}




