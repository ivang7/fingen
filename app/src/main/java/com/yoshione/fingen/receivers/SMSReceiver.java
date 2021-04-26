package com.yoshione.fingen.receivers;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.telephony.SmsMessage;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.yoshione.fingen.ActivityEditTransaction;
import com.yoshione.fingen.ActivityMain;
import com.yoshione.fingen.BuildConfig;
import com.yoshione.fingen.FgConst;
import com.yoshione.fingen.R;
import com.yoshione.fingen.dao.AccountsDAO;
import com.yoshione.fingen.dao.SendersDAO;
import com.yoshione.fingen.dao.SmsDAO;
import com.yoshione.fingen.dao.TransactionsDAO;
import com.yoshione.fingen.managers.AccountManager;
import com.yoshione.fingen.managers.TransactionManager;
import com.yoshione.fingen.model.Account;
import com.yoshione.fingen.model.Cabbage;
import com.yoshione.fingen.model.Sender;
import com.yoshione.fingen.model.Sms;
import com.yoshione.fingen.model.Transaction;
import com.yoshione.fingen.utils.CabbageFormatter;
import com.yoshione.fingen.utils.NotificationCounter;
import com.yoshione.fingen.utils.NotificationHelper;
import com.yoshione.fingen.utils.PrefUtils;
import com.yoshione.fingen.utils.RequestCodes;
import com.yoshione.fingen.utils.SmsParser;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by slv on 29.10.2015.
 *
 */
public class SMSReceiver extends BroadcastReceiver {
    private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final int NOTIFICATION_ID_TRANSACTION_AUTO_CREATED = 0;
    private static String sLastSms = "";
    private static long sLastSmsTime = 0;
//    private Intent mIntent;

    @SuppressLint("StringFormatInvalid")
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null && action.equals(ACTION_SMS_RECEIVED)) {
            String address = null;
            StringBuilder str = new StringBuilder();
            SmsMessage[] msgs = getMessagesFromIntent(intent);

            if (msgs != null) {
                for (SmsMessage msg : msgs) {
                    address = msg.getOriginatingAddress();
                    str.append(msg.getMessageBody());
                }
            }

            if (sLastSms.equals(str.toString()) & (System.currentTimeMillis() - sLastSmsTime) < 3000) {
                return;
            } else {
                sLastSms = str.toString();
                sLastSmsTime = System.currentTimeMillis();
            }

            if (BuildConfig.DEBUG) {
                if (address != null && address.equals("123456789")) {
                    address = "mtbank";
                }
            }

            Sender sender = SendersDAO.getInstance(context).getSenderByPhoneNo(address);

            if (sender.getID() >= 0) {
                Sms sms = new Sms(-1, new Date(), sender.getID(), str.toString());
                parseSMS(context, sms);
            }
        }
    }

    public void parseSMS(Context context, Sms sms) {
        SmsParser smsParser = new SmsParser(sms, context);
        if (smsParser.goodToParse()) {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("auto_create_transaction_on_full_set_of_patterns",true)){
                Transaction transaction = smsParser.extractTransaction();
                if (TransactionManager.isValidToSmsAutocreate(transaction, context)) {
                    TransactionsDAO transactionsDAO = TransactionsDAO.getInstance(context);
                    transaction.setComment(sms.getmBody());
                    transaction.setAutoCreated(true);
                    try {
                        transaction = (Transaction) transactionsDAO.createModel(transaction);
                    } catch (Exception e) {
                        return;
                    }

                    Intent notificationIntent = new Intent(context, ActivityMain.class);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    notificationIntent.putExtra("action", ActivityMain.ACTION_OPEN_TRANSACTIONS_LIST);
                    PendingIntent contentIntent = PendingIntent.getActivity(context,
                            0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                    Resources res = context.getResources();
                    Notification.Builder builder = NotificationHelper.getInstance(context).getNotification();

                    NotificationCounter notificationCounter = new NotificationCounter(PreferenceManager.getDefaultSharedPreferences(context));
                    int notificationCount = notificationCounter.getNotificationCount(NOTIFICATION_ID_TRANSACTION_AUTO_CREATED);
                    builder.setContentIntent(contentIntent)
                            .setSmallIcon(R.drawable.ic_notyfication)
                            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_main))// большая картинка
                            .setTicker(res.getString(R.string.notif_ticker_transaction_auto_created))// текст в строке состояния
                            .setWhen(new Date().getTime())
                            .setAutoCancel(true);

                    builder.setColor(ContextCompat.getColor(context, R.color.ColorAccent));
                    if (notificationCount == 0) {
                        Intent intentEdit = new Intent(context, ActivityEditTransaction.class);
                        intentEdit.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intentEdit.putExtra("transaction", transaction);
                        intentEdit.putExtra("EXIT", true);
                        PendingIntent pIntentEdit = PendingIntent.getActivity(context,
                                RequestCodes.REQUEST_CODE_EDIT_TRANSACTION_FROM_NOTIFYCATION, intentEdit, PendingIntent.FLAG_CANCEL_CURRENT);

                        builder//.setContentTitle(res.getString(R.string.notif_ticker_transaction_auto_created))// Заголовок уведомления
                                .setContentText(TransactionManager.transactionToString(transaction, context))// Текст уведомления
                                .addAction(android.R.drawable.ic_menu_edit, context.getString(R.string.act_edit), pIntentEdit);

                        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(FgConst.PREF_SCAN_QR_ENABLED, true)) {
                            Intent intentScanQR = new Intent(context, ActivityEditTransaction.class);
                            intentScanQR.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intentScanQR.putExtra("transaction", transaction);
                            intentScanQR.putExtra("scan_qr", true);
                            intentScanQR.putExtra("EXIT", true);
                            PendingIntent pIntentScanQR = PendingIntent.getActivity(context,
                                    RequestCodes.REQUEST_CODE_SCAN_QR, intentScanQR, PendingIntent.FLAG_CANCEL_CURRENT);
                            builder.addAction(R.drawable.ic_scan_qr_gray, context.getString(R.string.ttl_scan_qr), pIntentScanQR);
                        }
                    } else {
                        builder.setContentText(res.getString(R.string.notif_count_of_new_transactions))
                                .setNumber(notificationCount + 1); // Текст уведомления
                    }
                    notificationCounter.addNotification(NOTIFICATION_ID_TRANSACTION_AUTO_CREATED);

                    NotificationHelper.getInstance(context).notify(NOTIFICATION_ID_TRANSACTION_AUTO_CREATED, builder);

                    //--------------------

                    //Проверяем ошибку баланса и показываем уведомление с предложением создать
                    //корректирующую транзакцию
                    Account account = AccountsDAO.getInstance(context).getAccountByID(transaction.getAccountID());
                    Cabbage cabbage = AccountManager.getCabbage(account, context);
                    BigDecimal balanceError = smsParser.checkBalance(account);
                    if (balanceError.compareTo(BigDecimal.ZERO) != 0) {
                        //Создаем новую транзакцию с тем же счетом, что и в автоматически созданной.
                        //Сумму указываем ошибку баланса
                        Transaction cortr = new Transaction(PrefUtils.getDefDepID(context));
                        cortr.setAmount(balanceError, balanceError.compareTo(BigDecimal.ZERO));
                        cortr.setAccountID(transaction.getAccountID());

                        /*Создаем интент, открывающий редактор транзакций*/
                        notificationIntent = new Intent(context, ActivityEditTransaction.class);
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        notificationIntent.putExtra("transaction", cortr);

                        /*Pending intent*/
                        contentIntent = PendingIntent.getActivity(context,
                                RequestCodes.REQUEST_CODE_EDIT_TRANSACTION, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                        /*Строим уведомление*/
                        String s = res.getString(R.string.notif_create_correcting_transaction);
                        s = String.format(s, account.getName(), new CabbageFormatter(cabbage).format(balanceError));
//                                builder = new NotificationCompat.Builder(context);
                        builder = NotificationHelper.getInstance(context).getNotification();
                        builder.setContentIntent(contentIntent)
                                .setSmallIcon(R.drawable.ic_notyfication)
                                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_main))// большая картинка
                                .setTicker(res.getString(R.string.notif_create_correcting_transaction_caption))// текст в строке состояния
                                .setWhen(new Date().getTime())
                                .setAutoCancel(true)
                                .setStyle(new Notification.BigTextStyle().bigText(s))
                                .setContentTitle(res.getString(R.string.notif_create_correcting_transaction_caption))// Заголовок уведомления
                                /*.setContentText(s)*/; // Текст уведомления

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            builder.setColor(ContextCompat.getColor(context, R.color.ColorMain));
                        }

//                                notification = builder.build();

                        int notifId = 100 + (int) account.getID();
                        notificationCounter.addNotification(notifId);

//                                notificationManager.notify(notifId, notification);
                        NotificationHelper.getInstance(context).notify(notifId, builder);
                    }

                } else {
                    SmsDAO smsDAO = SmsDAO.getInstance(context);
                    try {
                        smsDAO.createModel(sms);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                SmsDAO smsDAO = SmsDAO.getInstance(context);
                try {
                    smsDAO.createModel(sms);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        if (messages != null) {
            byte[][] pduObjs = new byte[messages.length][];

            for (int i = 0; i < messages.length; i++) {
                pduObjs[i] = (byte[]) messages[i];
            }

            byte[][] pdus = new byte[pduObjs.length][];
            int pduCount = pdus.length;
            SmsMessage[] msgs = new SmsMessage[pduCount];

            for (int i = 0; i < pduCount; i++) {
                pdus[i] = pduObjs[i];
                msgs[i] = SmsMessage.createFromPdu(pdus[i]);
            }

            return msgs;
        } else {
            return null;
        }
    }

}
