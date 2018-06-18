package org.nervos.neuron.service;


import android.content.Context;
import android.text.TextUtils;

import org.nervos.neuron.R;
import org.nervos.neuron.item.TokenItem;
import org.nervos.neuron.util.LogUtil;
import org.nervos.neuron.util.db.DBWalletUtil;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Int64;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.nervos.neuron.util.ConstantUtil.ETHDecimal;

public class EthRpcService extends BaseRpcService {

    public static void init(Context context) {
        if (service == null) {
            service = Web3jFactory.build(new InfuraHttpService(ETH_NODE_IP));
        }
        walletItem = DBWalletUtil.getCurrentWallet(context);
    }

    public static EthGetBalance getEthBalance(String address) {
        try {
            return service.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TokenItem getDefaultEth(String address) {
        EthGetBalance ethGetBalance = EthRpcService.getEthBalance(address);
        if (ethGetBalance != null) {
            double balance = ethGetBalance.getBalance().multiply(BigInteger.valueOf(10000))
                    .divide(ETHDecimal).doubleValue()/10000.0;
            LogUtil.d("eth balanceOf: " + balance);
            return new TokenItem(ETH, R.drawable.ethereum, balance, -1);
        }
        return null;
    }

    public static Observable<BigInteger> getEthGasPrice() {
        return Observable.fromCallable(new Callable<BigInteger>() {
            @Override
            public BigInteger call() {
                BigInteger gasPrice = Numeric.toBigInt("0x4E3B29200");
                try {
                    gasPrice = service.ethGasPrice().send().getGasPrice();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LogUtil.d("gasPrice: " + gasPrice.toString());
                return gasPrice;
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<EthSendTransaction> transferEth(String address, double value, BigInteger gasPrice) {
        return Observable.fromCallable(new Callable<BigInteger>() {
            @Override
            public BigInteger call() throws Exception {
                EthGetTransactionCount ethGetTransactionCount = service
                        .ethGetTransactionCount(walletItem.address, DefaultBlockParameterName.LATEST).send();
                return ethGetTransactionCount.getTransactionCount();
            }
        }).flatMap(new Func1<BigInteger, Observable<String>>() {
            @Override
            public Observable<String> call(BigInteger nonce) {
                Credentials credentials = Credentials.create(walletItem.privateKey);
                BigInteger transferValue = ETHDecimal
                        .multiply(BigInteger.valueOf((long)(10000*value))).divide(BigInteger.valueOf(10000));
                RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce,
                        gasPrice, GAS_LIMIT, address, transferValue);
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                return Observable.just(Numeric.toHexString(signedMessage));
            }
        }).flatMap(new Func1<String, Observable<EthSendTransaction>>() {
            @Override
            public Observable<EthSendTransaction> call(String hexValue){
                try {
                    EthSendTransaction ethSendTransaction =
                            service.ethSendRawTransaction(hexValue).sendAsync().get();
                    return Observable.just(ethSendTransaction);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Observable.just(null);
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }


    public static void getTransactionReceipt(String hash) {
        Observable.fromCallable(new Callable<EthGetTransactionReceipt>() {
            @Override
            public EthGetTransactionReceipt call() {
                try {
                    return service.ethGetTransactionReceipt(hash).send();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<EthGetTransactionReceipt>() {
            @Override
            public void onCompleted() {

            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
            @Override
            public void onNext(EthGetTransactionReceipt ethGetTransactionReceipt) {
                LogUtil.d("transaction receipt: " + ethGetTransactionReceipt.getTransactionReceipt());
            }
        });
    }


    /**
     * get standard erc20 token info through function hash and parameters
     */
    public static TokenItem getTokenInfo(String contractAddress, String address) {
        try {
            return new TokenItem(getErc20Name(address, contractAddress),
                    getErc20Symbol(address, contractAddress), getErc20Decimal(address, contractAddress),
                    contractAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static double getERC20Balance(String contractAddress, String address) {
        try {
            long decimal = getErc20Decimal(address, contractAddress);

            Transaction balanceCall = Transaction.createEthCallTransaction(address, contractAddress,
                    BALANCEOF_HASH + ZERO_16 + Numeric.cleanHexPrefix(address));
            String balanceOf = service.ethCall(balanceCall, DefaultBlockParameterName.LATEST).send().getValue();
            LogUtil.d("erc20 balanceOf: " + balanceOf);
            if (!TextUtils.isEmpty(balanceOf) && !"0x".equals(balanceOf)) {
                initIntTypes();
                Int64 balance = (Int64) FunctionReturnDecoder.decode(balanceOf, intTypes).get(0);
                double balances = balance.getValue().doubleValue();
                if (decimal == 0) return balances;
                else return balances/(Math.pow(10, decimal));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }


    public static Observable<EthSendTransaction> transferErc20(TokenItem tokenItem, String address,
                                                               double value, BigInteger gasPrice) {
        BigInteger transferValue = getTransferValue(tokenItem, value);
        String data = createTokenTransferData(address, transferValue);
        return Observable.fromCallable(new Callable<BigInteger>() {
            @Override
            public BigInteger call() throws Exception {
                EthGetTransactionCount ethGetTransactionCount = service
                        .ethGetTransactionCount(walletItem.address, DefaultBlockParameterName.LATEST).send();
                return ethGetTransactionCount.getTransactionCount();
            }
        }).flatMap(new Func1<BigInteger, Observable<String>>() {
            @Override
            public Observable<String> call(BigInteger nonce) {
                Credentials credentials = Credentials.create(walletItem.privateKey);
                RawTransaction rawTransaction = RawTransaction.createTransaction(nonce,
                        gasPrice, Numeric.toBigInt("0x23280"), tokenItem.contractAddress, data);
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                return Observable.just(Numeric.toHexString(signedMessage));
            }
        }).flatMap(new Func1<String, Observable<EthSendTransaction>>() {
            @Override
            public Observable<EthSendTransaction> call(String hexValue){
                try {
                    EthSendTransaction ethSendTransaction =
                            service.ethSendRawTransaction(hexValue).sendAsync().get();
                    return Observable.just(ethSendTransaction);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Observable.just(null);
            }
        }).filter(ethSendTransaction -> ethSendTransaction != null
                && !TextUtils.isEmpty(ethSendTransaction.getTransactionHash()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    private static BigInteger getTransferValue(TokenItem tokenItem, double value) {
        StringBuilder sb = new StringBuilder("1");
        for(int i = 0; i < tokenItem.decimals; i++) {
            sb.append("0");
        }
        BigInteger ERC20Decimal = new BigInteger(sb.toString());
        return ERC20Decimal.multiply(BigInteger.valueOf((long)(10000*value)))
                .divide(BigInteger.valueOf(10000));
    }


    private static String createTokenTransferData(String to, BigInteger tokenAmount) {
        List<Type> params = Arrays.<Type>asList(new Address(to), new Uint256(tokenAmount));

        List<TypeReference<?>> returnTypes = Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
        });

        Function function = new Function("transfer", params, returnTypes);
        return FunctionEncoder.encode(function);
    }

    private static String getErc20Name(String address, String contractAddress) throws IOException {
        Transaction nameCall = Transaction.createEthCallTransaction(address, contractAddress, NAME_HASH);
        String name = service.ethCall(nameCall, DefaultBlockParameterName.LATEST).send().getValue();
        if (TextUtils.isEmpty(name) || "0x".equals(name)) return null;
        initStringTypes();
        return FunctionReturnDecoder.decode(name, stringTypes).get(0).toString();
    }

    private static String getErc20Symbol(String address, String contractAddress) throws IOException {
        Transaction symbolCall = Transaction.createEthCallTransaction(address, contractAddress, SYMBOL_HASH);
        String symbol = service.ethCall(symbolCall, DefaultBlockParameterName.LATEST).send().getValue();
        if (TextUtils.isEmpty(symbol) || "0x".equals(symbol)) return null;
        initStringTypes();
        return FunctionReturnDecoder.decode(symbol, stringTypes).get(0).toString();
    }

    private static int getErc20Decimal(String address, String contractAddress) throws IOException {
        Transaction decimalsCall = Transaction.createEthCallTransaction(address, contractAddress, DECIMALS_HASH);
        String decimals = service.ethCall(decimalsCall, DefaultBlockParameterName.LATEST).send().getValue();
        if (!TextUtils.isEmpty(decimals) && !"0x".equals(decimals)) {
            initIntTypes();
            Int64 type = (Int64) FunctionReturnDecoder.decode(decimals, intTypes).get(0);
            return type.getValue().intValue();
        }
        return 0;
    }



}
