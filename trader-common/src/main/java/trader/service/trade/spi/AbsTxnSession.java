package trader.service.trade.spi;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import trader.common.beans.BeansContainer;
import trader.common.exception.AppException;
import trader.common.exchangeable.Exchangeable;
import trader.common.util.DateUtil;
import trader.service.ServiceConstants.ConnState;
import trader.service.md.MarketData;
import trader.service.trade.Account;
import trader.service.trade.Order;
import trader.service.trade.OrderStateTuple;
import trader.service.trade.Position;
import trader.service.trade.Transaction;
import trader.service.trade.TxnSession;

/**
 * 抽象的交易通道
 */
public abstract class AbsTxnSession implements TxnSession {

    protected BeansContainer beansContainer;
    protected Logger logger;
    protected Account account;
    protected TxnSessionListener listener;
    protected volatile ConnState state;
    protected long stateTime;
    protected LocalDate tradingDay;

    public AbsTxnSession(BeansContainer beansContainer, Account account, TxnSessionListener listener) {
        this.beansContainer = beansContainer;
        this.account = account;
        this.listener = listener;
        logger = LoggerFactory.getLogger(account.getLoggerPackage()+"."+getClass().getSimpleName());
        state = ConnState.Initialized;
        stateTime = System.currentTimeMillis();
    }

    @Override
    public ConnState getState() {
        return state;
    }

    public long getStatusTime() {
        return stateTime;
    }

    @Override
    public LocalDate getTradingDay() {
        return tradingDay;
    }

    /**
     * 异步连接
     */
    public abstract void connect();

    /**
     * 加载费率计算: 交易品种, 主力合约, 保证金率, 每跳幅度等等
     * <BR>这个函数在syncQryPositions之后调用, 可以获得实际期货公司的保证金率
     *
     * @param 关注的品种
     *
     * @return 返回JSON格式的费率数据
     */
    public abstract String syncLoadFeeEvaluator(Collection<Exchangeable> subscriptions) throws Exception;

    /**
     * 同步确认结算单
     */
    public abstract String syncConfirmSettlement() throws Exception ;

    /**
     * 同步查询账户接本账户数据
     */
    public abstract long[] syncQryAccounts() throws Exception;

    /**
     * 加载当前持仓品种, 并分配到AccountView.
     * <BR>注意, 查询中不得有在途交易, 否则会出现Position数据计算不对的问题
     */
    public abstract List<Position> syncQryPositions() throws Exception;

    /**
     * 发送报单
     */
    public abstract void asyncSendOrder(Order order) throws AppException;

    /**
     * 查询市场所有合约
     */
    public abstract List<MarketData> syncQueryMarketDatas() throws Exception;

    protected abstract void closeImpl();

    public void close() {
        closeImpl();
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("state", state.name());
        json.addProperty("stateTime", stateTime);
        if ( tradingDay!=null ) {
            json.addProperty("tradingDay", DateUtil.date2str(tradingDay));
        }
        return json;
    }

    /**
     * 由CTP实现类调用, 用于设置新状态
     */
    protected void changeState(ConnState newState) {
        if ( newState!=state ) {
            ConnState lastState = state;
            state = newState;
            stateTime = System.currentTimeMillis();
            logger.info(account.getId()+" status changes from "+lastState+" to "+state);
            listener.onTxnSessionStateChanged(this, lastState);
        }
    }

    /**
     * 由CTP实现类调用, 用于设置报单新状态
     */
    protected void orderChangeState(Order order, OrderStateTuple newState) {
        OrderStateTuple oldState = listener.changeOrderState(order, newState);
        if ( oldState!=null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Order "+order.getRef()+" change state to "+newState+", last state: "+oldState);
            }
        } else {
            logger.info("Order "+order.getRef()+" is failed to change to new state: "+newState+", last state: "+order.getState());
        }
    }

    /**
     * 由实现类调用, 用于设置订单成交
     */
    protected void orderAppendTxn(Order order, Transaction txn) {
        listener.onTransaction(order, txn, System.currentTimeMillis());
    }

}