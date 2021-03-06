package awais.instagrabber.asyncs.direct_messages;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.models.enums.UserInboxDirection;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.logCollector;
import static awaisomereport.LogCollector.LogFile;

public final class UserInboxFetcher extends AsyncTask<Void, Void, InboxThreadModel> {
    private final String id;
    private final String endCursor;
    private final FetchListener<InboxThreadModel> fetchListener;
    private final String direction;

    public UserInboxFetcher(final String id, final UserInboxDirection direction, final String endCursor,
                            final FetchListener<InboxThreadModel> fetchListener) {
        this.id = id;
        this.direction = "&direction=" + (direction == UserInboxDirection.NEWER ? "newer" : "older");
        this.endCursor = !Utils.isEmpty(endCursor) ? "&cursor=" + endCursor : "";
        this.fetchListener = fetchListener;
    }

    @Nullable
    @Override
    protected InboxThreadModel doInBackground(final Void... voids) {
        InboxThreadModel result = null;
        final String url = "https://i.instagram.com/api/v1/direct_v2/threads/" + id + "/?visual_message_return_type=unseen"
                + direction + endCursor;
        // todo probably
        //  &   seq_id = seqId

        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", Constants.USER_AGENT);
            conn.setUseCaches(false);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final JSONObject data = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("thread");
                result = Utils.createInboxThreadModel(data, true);
            }

            conn.disconnect();
        } catch (final Exception e) {
            result = null;
            if (logCollector != null)
                logCollector.appendException(e, LogFile.ASYNC_DMS_THREAD, "doInBackground");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final InboxThreadModel inboxThreadModel) {
        if (fetchListener != null) fetchListener.onResult(inboxThreadModel);
    }
}