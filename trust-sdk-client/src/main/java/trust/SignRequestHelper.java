package trust;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import java.util.HashSet;
import java.util.Set;

import trust.core.entity.Message;
import trust.core.entity.Transaction;

public class SignRequestHelper implements Parcelable {
    private Request request;

    private static final Set<String> hosts = new HashSet<String>() {{
        add(Trust.ACTION_SIGN_TRANSACTION);
        add(Trust.ACTION_SIGN_MESSAGE);
        add(Trust.ACTION_SIGN_PERSONAL_MESSAGE);

    }};

    public SignRequestHelper(Intent intent, Callback callback) {
        final Uri uri = intent.getData();
        if (!isSignUri(uri)) {
            return;
        }
        final String action = uri.getAuthority();

        switch (action) {
            case Trust.ACTION_SIGN_PERSONAL_MESSAGE:
            case Trust.ACTION_SIGN_MESSAGE: {
                request = SignMessageRequest.builder().uri(uri).get();
                Message message = request.body();
                if (message.isPersonal) {
                    callback.signPersonalMessage(message);
                } else {
                    callback.signMessage(message);
                }
            } break;
            case Trust.ACTION_SIGN_TRANSACTION: {
                request = SignTransactionRequest.builder().uri(uri).get();
                callback.signTransaction((Transaction) request.body());
            } break;
        }
    }

    protected SignRequestHelper(Parcel in) {
        request = in.readParcelable(Request.class.getClassLoader());
    }

    public void onSignCancel(Activity activity) {
        Intent intent = new Intent(request.getAction());
        intent.setData(request.key());
        activity.setResult(Activity.RESULT_CANCELED, intent);
        activity.finish();
    }

    public void onSignError(Activity activity, int error) {
        fail(activity, error);
    }

    private void fail(Activity activity, int error) {
        Intent intent = new Intent(request.getAction());
        if (request.getCallbackUri() != null) {
            Uri uri = request.getCallbackUri().buildUpon()
                    .appendQueryParameter("src", getSrcUri(request.key()))
                    .appendQueryParameter("error", String.valueOf(error))
                    .build();
            intent.setData(uri);
            activity.startActivity(intent);
            activity.finish();
        } else {
            intent.setData(request.key());
            intent.putExtra(Trust.ExtraKey.ERROR, error);
            activity.setResult(Trust.RESULT_ERROR, intent);
            activity.finish();
        }
    }

    public void onMessageSigned(Activity activity, String signHex) {
        success(activity, signHex);
    }

    public void onTransactionSigned(Activity activity, String signHex) {
        success(activity, signHex);
    }

    private void success(Activity activity, String signHex) {
        Intent intent = new Intent(request.getAction());
        if (request.getCallbackUri() != null) {
            Uri uri = request.getCallbackUri().buildUpon()
                    .appendQueryParameter("src", getSrcUri(request.key()))
                    .appendQueryParameter("result", signHex)
                    .build();
            intent.setData(uri);
            activity.startActivity(intent);
            activity.finish();
        } else {
            intent.setData(request.key());
            intent.putExtra(Trust.ExtraKey.SIGN, signHex);
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
        }
    }

    private String getSrcUri(Uri key) {
        return new String(Base64.encode(key.toString().getBytes(), Base64.DEFAULT));
    }

    private static boolean isSignUri(Uri uri) {
        return uri != null && "trust".equals(uri.getScheme())
                && hosts.contains(uri.getHost());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(request, flags);
    }

    public static final Creator<SignRequestHelper> CREATOR = new Creator<SignRequestHelper>() {
        @Override
        public SignRequestHelper createFromParcel(Parcel in) {
            return new SignRequestHelper(in);
        }

        @Override
        public SignRequestHelper[] newArray(int size) {
            return new SignRequestHelper[size];
        }
    };

    public interface Callback {
        void signMessage(Message message);

        void signPersonalMessage(Message message);

        void signTransaction(Transaction transaction);
    }
}
