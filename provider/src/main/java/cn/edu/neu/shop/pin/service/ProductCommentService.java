package cn.edu.neu.shop.pin.service;

import cn.edu.neu.shop.pin.exception.CommentFailedException;
import cn.edu.neu.shop.pin.exception.PermissionDeniedException;
import cn.edu.neu.shop.pin.mapper.PinProductAttributeValueMapper;
import cn.edu.neu.shop.pin.mapper.PinUserProductCommentMapper;
import cn.edu.neu.shop.pin.model.PinOrderIndividual;
import cn.edu.neu.shop.pin.model.PinUserProductComment;
import cn.edu.neu.shop.pin.nlp.NLPUtil;
import cn.edu.neu.shop.pin.util.base.AbstractService;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class ProductCommentService extends AbstractService<PinUserProductComment> {

    private final PinUserProductCommentMapper pinUserProductCommentMapper;

    private final OrderIndividualService orderIndividualService;

    private final PinProductAttributeValueMapper pinProductAttributeValueMapper;

    @Autowired
    public ProductCommentService(PinUserProductCommentMapper pinUserProductCommentMapper, OrderIndividualService orderIndividualService, PinProductAttributeValueMapper pinProductAttributeValueMapper) {
        this.pinUserProductCommentMapper = pinUserProductCommentMapper;
        this.orderIndividualService = orderIndividualService;
        this.pinProductAttributeValueMapper = pinProductAttributeValueMapper;
    }

    /**
     * @param userId   用户ID
     * @param comments 评论对象集合
     * @author flyhero
     * 为商品添加评论
     */
    @Transactional
    public void addComments(Integer userId, List<PinUserProductComment> comments) throws PermissionDeniedException, CommentFailedException {
        // 传进来数组为空
        if (comments == null || comments.size() == 0) return;
        // 得到orderIndividual
        Integer orderIndividualId = comments.get(0).getOrderIndividualId();
        Integer skuId = comments.get(0).getSkuId();
        PinOrderIndividual orderIndividual = orderIndividualService.findById(orderIndividualId);
        if (!Objects.equals(userId, orderIndividual.getUserId())) { // 用户ID不符
            throw new PermissionDeniedException("Caused by ProductCommentService: 评论用户与订单所属用户ID不符！");
        } else if (orderIndividual.getStatus() != PinOrderIndividual.STATUS_PENDING_COMMENT) {
            // 订单状态不是"待评价"
            throw new CommentFailedException("Caused by ProductCommentService: 添加评论失败，订单状态异常！");
        }
        // 评价此订单中的所有商品（精确到sku）
        for (PinUserProductComment comment : comments) {
            // 检查是否已评论
            PinUserProductComment commentSample = new PinUserProductComment();
            commentSample.setOrderIndividualId(orderIndividualId);
            commentSample.setSkuId(skuId);
            PinUserProductComment checkIfExists = pinUserProductCommentMapper.selectOne(commentSample);
            // 加入评论观点分析
            try {
                comment.setCommentTag(NLPUtil.analyzeCommentTag(comment.getContent()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (checkIfExists != null) { // 如果评论已存在，则覆盖更新这条评论
                comment.setId(checkIfExists.getId());
                this.update(comment);
                // 更新订单状态
                orderIndividual.setStatus(PinOrderIndividual.STATUS_COMMENTED);
                orderIndividualService.update(orderIndividual);
            } else {
                // 新鲜的评论
                // 由于前端只返回了skuId而没有返回productId，因此需要根据skuId找到其对应的productId
                comment.setProductId(pinProductAttributeValueMapper.selectByPrimaryKey(skuId).getProductId());
                comment.setUserId(userId);
                comment.setCreateTime(new Date());
                pinUserProductCommentMapper.insert(comment); // 评论表中新增一条记录
                // 更新订单状态为已评价
                orderIndividual.setStatus(PinOrderIndividual.STATUS_COMMENTED);
                orderIndividualService.update(orderIndividual);
            }
        }
    }

    /**
     * 根据商品ID 获取该商品评论信息
     *
     * @param productId 商品 ID
     * @return List
     */
    public PageInfo<PinUserProductComment> getCommentByProductIdByPage(Integer productId, Integer pageNum, Integer pageSize) {
        return PageHelper.startPage(pageNum, pageSize).doSelectPageInfo(() -> pinUserProductCommentMapper.getCommentAndUserInfo(productId));
    }

    /**
     * 从当前时间算起，获取之前7天内每天的评论数
     *
     * @param storeId 店铺ID
     * @return 获取评论数成功与否的状态
     */
    public Integer[] getComments(Integer storeId) {
        Integer[] comment = new Integer[7];
        Date date = new Date();
        date = getDateByOffset(date, 1);
        for (int i = 0; i < 7; i++) {
            Date toDate = date;
            date = getDateByOffset(date, -1);
            Date fromDate = date;
//            System.out.println("fromDate: " + fromDate + " --- toDate: " + toDate);
            comment[i] = pinUserProductCommentMapper.getNumberOfComment(fromDate, toDate, storeId);
//            System.out.println("comment[i]: " + comment[i]);
        }
        return comment;
    }

    /**
     * 管理端
     * 分页获取商品评论信息 包括评论用户昵称和头像 购买商品类型
     */
    public List<JSONObject> getCommentAndUserInfoByPage(Integer productId) {
        return pinUserProductCommentMapper.getCommentAndUserInfo(productId);
    }

    /**
     * @param list       列表长度
     * @param pageNumber 分页数量
     * @param pageSize   页面长度
     * @return 评论列表
     */
    public List getCommentsByPageNumAndSize(List list, Integer pageNumber, Integer pageSize) {
        if (pageNumber * pageSize < list.size()) {
            return list.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);
        } else {
            return list.subList((pageNumber - 1) * pageSize, list.size());
        }
    }

    /**
     * 获取该店铺商家未评论总数
     *
     * @param storeId 店铺ID
     * @return 未评论的店家数量
     */
    public Integer getMerchantNotComment(Integer storeId) {
        return pinUserProductCommentMapper.getNumberOfMerchantNotComment(storeId);
    }

    /**
     * 指定偏移的天数，计算某天的日期
     *
     * @param today 当前时间
     * @param delta 偏移量
     * @return 希望得到的日期
     */
    private java.util.Date getDateByOffset(java.util.Date today, Integer delta) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + delta);
        return calendar.getTime();
    }

    /**
     * 获取某一店铺内尚未评论的产品列表
     *
     * @param storeId 店铺ID
     * @return 商品列表
     */
    public List<JSONObject> getProductWithComment(Integer storeId) {
        return pinUserProductCommentMapper.getAllProductWithComment(storeId);
    }

    public void updateMerchantCommentContent(Integer commentId, String commentContent, Date commentTime) {
        pinUserProductCommentMapper.updateMerchantComment(commentId, commentContent, commentTime);
    }
}
