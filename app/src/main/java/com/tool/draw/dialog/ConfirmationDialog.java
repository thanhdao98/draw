package com.tool.draw.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public final class ConfirmationDialog {
    public static void show(Context context, DialogInterface.OnClickListener positiveListener) {
        new AlertDialog.Builder(context).setMessage("作成した画像を送信しますか?")
                .setPositiveButton("はい", positiveListener)
                .setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create().show();
    }
}
