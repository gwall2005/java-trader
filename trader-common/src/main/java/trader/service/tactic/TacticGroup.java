package trader.service.tactic;

import java.util.List;
import java.util.Properties;

import trader.common.exchangeable.Exchangeable;
import trader.service.trade.AccountView;

/**
 * 交易策略分组, 一组开平仓策略以及相关配置参数构成一个完整的交易模型
 */
public interface TacticGroup {

    /**
     * 分组ID
     */
    public String getId();

    /**
     * 账户视图
     */
    public AccountView getAccountView();

    /**
     * 可交易品种
     */
    public List<Exchangeable> getExchangeable();

    /**
     * 配置参数
     */
    public Properties getProperties();

    /**
     * 交易策略列表
     */
    public List<Tactic> getTactics();

    /**
     * 是否启用
     */
    public boolean isEnabled();

    /**
     * 设置启用/禁用
     */
    public void setEnabled(boolean value);

}