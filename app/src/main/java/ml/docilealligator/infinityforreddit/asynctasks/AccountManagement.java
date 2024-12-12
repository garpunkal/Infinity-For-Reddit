package ml.docilealligator.infinityforreddit.asynctasks;

import static ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils.POST_LAYOUT_CARD_2;
import static ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils.POST_LAYOUT_GALLERY;

import android.content.SharedPreferences;
import android.os.Handler;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executor;

import ml.docilealligator.infinityforreddit.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.account.Account;
import ml.docilealligator.infinityforreddit.account.AccountDao;
import ml.docilealligator.infinityforreddit.events.ChangePostLayoutEvent;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;

public class AccountManagement {

    public static void switchAccount(RedditDataRoomDatabase redditDataRoomDatabase,
                                     SharedPreferences currentAccountSharedPreferences, Executor executor,
                                     Handler handler, String newAccountName,
                                     SwitchAccountListener switchAccountListener) {
        executor.execute(() -> {
            redditDataRoomDatabase.accountDao().markAllAccountsNonCurrent();
            redditDataRoomDatabase.accountDao().markAccountCurrent(newAccountName);
            Account account = redditDataRoomDatabase.accountDao().getCurrentAccount();
            currentAccountSharedPreferences.edit()
                    .putString(SharedPreferencesUtils.ACCESS_TOKEN, account.getAccessToken())
                    .putString(SharedPreferencesUtils.ACCOUNT_NAME, account.getAccountName())
                    .putString(SharedPreferencesUtils.ACCOUNT_IMAGE_URL, account.getProfileImageUrl()).apply();
            currentAccountSharedPreferences.edit().remove(SharedPreferencesUtils.SUBSCRIBED_THINGS_SYNC_TIME).apply();
            handler.post(() -> switchAccountListener.switched(account));

            // hack for default post layout for user
            if (newAccountName.startsWith("dextergood")) {
                EventBus.getDefault().post(new ChangePostLayoutEvent(POST_LAYOUT_GALLERY));
            }
            //\endhack
        });

    }

    public static void switchToAnonymousMode(RedditDataRoomDatabase redditDataRoomDatabase, SharedPreferences currentAccountSharedPreferences,
                                             Executor executor, Handler handler, boolean removeCurrentAccount,
                                             SwitchToAnonymousAccountAsyncTaskListener switchToAnonymousAccountAsyncTaskListener) {
        executor.execute(() -> {
            AccountDao accountDao = redditDataRoomDatabase.accountDao();
            if (removeCurrentAccount) {
                accountDao.deleteCurrentAccount();
            }
            accountDao.markAllAccountsNonCurrent();

            String redgifsAccessToken = currentAccountSharedPreferences.getString(SharedPreferencesUtils.REDGIFS_ACCESS_TOKEN, "");

            currentAccountSharedPreferences.edit().clear().apply();
            currentAccountSharedPreferences.edit().putString(SharedPreferencesUtils.REDGIFS_ACCESS_TOKEN, redgifsAccessToken).apply();

            handler.post(switchToAnonymousAccountAsyncTaskListener::logoutSuccess);
        });
    }

    public static void removeAccount(RedditDataRoomDatabase redditDataRoomDatabase,
                                     Executor executor, String accoutName) {
        executor.execute(() -> {
            redditDataRoomDatabase.accountDao().deleteAccount(accoutName);
        });
    }

    public interface SwitchToAnonymousAccountAsyncTaskListener {
        void logoutSuccess();
    }

    public interface SwitchAccountListener {
        void switched(Account account);
    }
}
