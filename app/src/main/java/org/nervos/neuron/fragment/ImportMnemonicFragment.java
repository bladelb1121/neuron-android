package org.nervos.neuron.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import org.nervos.neuron.R;
import org.nervos.neuron.item.WalletItem;
import org.nervos.neuron.util.DBUtil;
import org.nervos.neuron.util.crypto.WalletEntity;
import org.web3j.crypto.CipherException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportMnemonicFragment extends Fragment {

    List<String> formats;
    List<String> paths;
    String currentPath;
    AppCompatSpinner spinner;
    ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    private AppCompatEditText walletNameEdit;
    private AppCompatEditText passwordEdit;
    private AppCompatEditText rePasswordEdit;
    private AppCompatEditText mnemonicEdit;
    private AppCompatButton importButton;
    private RadioButton radioButton;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import_mnemonic, container, false);
        spinner = view.findViewById(R.id.spinner_format);
        importButton = view.findViewById(R.id.import_mnemonic_button);
        walletNameEdit = view.findViewById(R.id.edit_wallet_name);
        passwordEdit = view.findViewById(R.id.edit_wallet_password);
        rePasswordEdit = view.findViewById(R.id.edit_wallet_repassword);
        mnemonicEdit = view.findViewById(R.id.edit_wallet_mnemonic);
        radioButton = view.findViewById(R.id.wallet_radio);
        progressBar = view.findViewById(R.id.progress_bar);
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        checkWalletStatus();
        initListener();
    }

    private void initView() {
        formats = Arrays.asList(getResources().getStringArray(R.array.mnemonic_format));
        paths = Arrays.asList(getResources().getStringArray(R.array.mnemonic_path));
        currentPath = paths.get(0);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, formats);
        spinner.setAdapter(adapter);

    }

    private void initListener() {
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.equals(passwordEdit.getText().toString().trim(),
                        rePasswordEdit.getText().toString().trim())) {
                    Toast.makeText(getContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    cachedThreadPool.execute(() -> {
                        try {
                            WalletEntity walletEntity = WalletEntity.fromMnemonic(
                                    mnemonicEdit.getText().toString().trim(), currentPath);
                            WalletItem walletItem = WalletItem.fromWalletEntity(walletEntity);
                            walletItem.name = walletNameEdit.getText().toString().trim();
                            walletItem.password = passwordEdit.getText().toString().trim();
                            DBUtil.saveWallet(getContext(), walletItem);

                            WalletItem walletItem1 = DBUtil.getCurrentWallet(getContext());

                            rePasswordEdit.post(() -> {
                                progressBar.setVisibility(View.INVISIBLE);
                                String name = walletItem1 == null? "wallet is null" : walletItem1.address;
                                Toast.makeText(getContext(), name, Toast.LENGTH_SHORT).show();
                            });
                        } catch (CipherException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPath = paths.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    private boolean isWalletValid() {
        return check1 && check2 && check3 && check4;
    }

    private void setCreateButtonStatus(boolean status) {
        importButton.setBackgroundResource(status?
                R.drawable.button_corner_blue_shape:R.drawable.button_corner_gray_shape);
        importButton.setEnabled(status);
    }


    private boolean check1 = false, check2 = false, check3 = false, check4 = false;
    private void checkWalletStatus() {
        walletNameEdit.addTextChangedListener(new WalletTextWatcher(){
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                super.onTextChanged(charSequence, i, i1, i2);
                check1 = !TextUtils.isEmpty(walletNameEdit.getText().toString().trim());
                setCreateButtonStatus(isWalletValid());
            }
        });
        passwordEdit.addTextChangedListener(new WalletTextWatcher(){
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                super.onTextChanged(charSequence, i, i1, i2);
                check2 = !TextUtils.isEmpty(passwordEdit.getText().toString().trim());
                setCreateButtonStatus(isWalletValid());
            }
        });
        rePasswordEdit.addTextChangedListener(new WalletTextWatcher(){
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                super.onTextChanged(charSequence, i, i1, i2);
                check3 = !TextUtils.isEmpty(rePasswordEdit.getText().toString().trim());
                setCreateButtonStatus(isWalletValid());
            }
        });
        radioButton.setOnCheckedChangeListener((compoundButton, b) ->{
            check4 = b;
            setCreateButtonStatus(isWalletValid());
        } );
    }


    private static class WalletTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }
        @Override
        public void afterTextChanged(Editable editable) {

        }
    }

}
