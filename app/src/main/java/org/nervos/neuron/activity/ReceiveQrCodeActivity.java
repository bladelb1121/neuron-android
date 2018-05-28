package org.nervos.neuron.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.uuzuche.lib_zxing.activity.CodeUtils;

import org.nervos.neuron.R;
import org.nervos.neuron.item.WalletItem;
import org.nervos.neuron.util.db.DBWalletUtil;

public class ReceiveQrCodeActivity extends AppCompatActivity {

    private ImageView qrCodeImage;
    private TextView walletNameText;
    private TextView walletAddressText;

    private ReceiveQrCodeActivity activity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_qrcode);

        activity = this;

        WalletItem walletItem = DBWalletUtil.getCurrentWallet(this);

        qrCodeImage = findViewById(R.id.receive_qrcode_image);
        walletNameText = findViewById(R.id.qrcode_wallet_name);
        walletAddressText = findViewById(R.id.qrcode_wallet_address);
        findViewById(R.id.button_copy_receive_qrcode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("qrCode", walletItem.address);
                if (cm != null) {
                    cm.setPrimaryClip(mClipData);
                    Toast.makeText(activity, "复制成功", Toast.LENGTH_SHORT).show();
                }
            }
        });

        walletNameText.setText(walletItem.name);
        walletAddressText.setText(walletItem.address);

        Bitmap bitmap = CodeUtils.createImage(walletItem.address, 400, 400, null);
        qrCodeImage.setImageBitmap(bitmap);

    }
}