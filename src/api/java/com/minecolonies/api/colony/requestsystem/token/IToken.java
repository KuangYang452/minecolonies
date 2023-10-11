package com.minecolonies.api.colony.requestsystem.token;

/**
 * 用于表示请求管理系统之外的请求的接口。
 * <p>
 * 允许简单地存储建筑物、工人等所有打开的请求，而无需两次存储整个请求。
 * <p>
 * 还扩展了INBTSerializable，以便更轻松地将数据写入磁盘。
 */
public interface IToken<T>
{

    /**
     * 用于表示请求的标识符。
     *
     * @return 代表此令牌的请求的标识符。
     */
    T getIdentifier();
}
