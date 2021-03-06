package org.houxg.leamonax.background;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.elvishew.xlog.XLog;

import org.greenrobot.eventbus.EventBus;
import org.houxg.leamonax.R;
import org.houxg.leamonax.model.SyncEvent;
import org.houxg.leamonax.network.LeaFailure;
import org.houxg.leamonax.service.AccountService;
import org.houxg.leamonax.service.NoteService;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by binnchx on 10/19/15.
 */
public class NoteSyncService extends Service {

    private static final String TAG = "NoteSyncService:";
    public static final String LEA_API_ERROR_NEED_UPGRADE_ACCOUNT = "NEED-UPGRADE-ACCOUNT";

    public static void startServiceForNote(Context context) {
        if (!AccountService.isSignedIn()) {
            XLog.w(TAG + "Trying to sync but not login");
            return;
        }

        Intent intent = new Intent(context, NoteSyncService.class);
        context.startService(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        Observable.create(
                new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            NoteService.fetchFromServer();
                            NoteService.pushToServer();
                            NoteService.buildFTSNote();
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Void>() {
                    @Override
                    public void onCompleted() {
                        stopSelf();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (e instanceof LeaFailure) {
                            LeaFailure leaFailure = (LeaFailure) e;
                            if (LEA_API_ERROR_NEED_UPGRADE_ACCOUNT.equals(leaFailure.getMessage())) {
                                EventBus.getDefault().post(new SyncEvent(false, getString(R.string.lea_api_error_need_upgrade_account)));
                            } else {
                                EventBus.getDefault().post(new SyncEvent(false));
                            }
                        } else {
                            EventBus.getDefault().post(new SyncEvent(false));
                        }
                        stopSelf();
                    }

                    @Override
                    public void onNext(Void val) {
                        EventBus.getDefault().post(new SyncEvent(true));
                    }
                });

        return START_NOT_STICKY;
    }
}
