package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.SuggestionModel;
import awais.instagrabber.models.enums.SuggestionType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.UrlEncoder;
import awais.instagrabber.utils.Utils;

public final class SuggestionsFetcher extends AsyncTask<String, String, SuggestionModel[]> {
    private final FetchListener<SuggestionModel[]> fetchListener;

    public SuggestionsFetcher(final FetchListener<SuggestionModel[]> fetchListener) {
        this.fetchListener = fetchListener;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected SuggestionModel[] doInBackground(final String... params) {
        SuggestionModel[] result = null;
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL("https://www.instagram.com/web/search/topsearch/?context=blended&count=50&query="
                    + UrlEncoder.encodeUrl(params[0])).openConnection();
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final String defaultHashTagPic = "https://www.instagram.com/static/images/hashtag/search-hashtag-default-avatar.png/1d8417c9a4f5.png";
                final JSONObject jsonObject = new JSONObject(Utils.readFromConnection(conn));
                conn.disconnect();

                final JSONArray usersArray = jsonObject.getJSONArray("users");
                final JSONArray hashtagsArray = jsonObject.getJSONArray("hashtags");

                final int usersLen = usersArray.length();
                final int hashtagsLen = hashtagsArray.length();

                final ArrayList<SuggestionModel> suggestionModels = new ArrayList<>(usersLen + hashtagsLen);
                for (int i = 0; i < hashtagsLen; i++) {
                    final JSONObject hashtagsArrayJSONObject = hashtagsArray.getJSONObject(i);

                    final JSONObject hashtag = hashtagsArrayJSONObject.getJSONObject("hashtag");

                    suggestionModels.add(new SuggestionModel(false,
                            hashtag.getString(Constants.EXTRAS_NAME),
                            null,
                            hashtag.optString("profile_pic_url", defaultHashTagPic),
                            SuggestionType.TYPE_HASHTAG,
                            hashtagsArrayJSONObject.optInt("position", suggestionModels.size() - 1)));
                }

                for (int i = 0; i < usersLen; i++) {
                    final JSONObject usersArrayJSONObject = usersArray.getJSONObject(i);

                    final JSONObject user = usersArrayJSONObject.getJSONObject(Constants.EXTRAS_USER);

                    suggestionModels.add(new SuggestionModel(user.getBoolean("is_verified"),
                            user.getString(Constants.EXTRAS_USERNAME),
                            user.getString("full_name"),
                            user.getString("profile_pic_url"),
                            SuggestionType.TYPE_USER,
                            usersArrayJSONObject.optInt("position", suggestionModels.size() - 1)));
                }

                suggestionModels.trimToSize();

                Collections.sort(suggestionModels);

                result = suggestionModels.toArray(new SuggestionModel[0]);
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG && !(e instanceof InterruptedIOException)) Log.e("AWAISKING_APP", "", e);
        }
        return result;
    }

    @Override
    protected void onPostExecute(final SuggestionModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}