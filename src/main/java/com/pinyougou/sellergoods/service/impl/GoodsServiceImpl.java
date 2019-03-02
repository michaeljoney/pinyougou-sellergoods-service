package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.*;
import com.pinyougou.pojo.TbGoodsExample.Criteria;
import com.pinyougou.pojogroup.Goods;
import com.pinyougou.sellergoods.service.GoodsService;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service(timeout = 2000)
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private TbGoodsMapper goodsMapper;
    private Long id;

    /**
     * 查询全部
     */
    @Override
    public List<TbGoods> findAll() {
        return goodsMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }


    @Autowired
    private TbGoodsDescMapper goodsDescMapper;
    @Autowired
    private TbItemMapper itemMapper;

    /**
     * 增加
     */
    @Override
    public void add(Goods goods) {
        goods.getGoods().setAuditStatus("0");//设置未申请状态
        goodsMapper.insert(goods.getGoods());
        goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());//设置ID
        goodsDescMapper.insert(goods.getGoodsDesc());//插入商品扩展数据
        if("1".equals(goods.getGoods().getIsEnableSpec())){
            for(TbItem item :goods.getItemList()){
                //标题
                String title= goods.getGoods().getGoodsName();
                Map<String,Object> specMap = JSON.parseObject(item.getSpec());
                for(String key:specMap.keySet()){
                    title+=" "+ specMap.get(key);
                }
                item.setTitle(title);
                setItemValus(goods,item);
                itemMapper.insert(item);
            }
        }else{
            TbItem item=new TbItem();
            item.setTitle(goods.getGoods().getGoodsName());//商品KPU+规格描述串作为SKU名称
            item.setPrice( goods.getGoods().getPrice() );//价格
            item.setStatus("1");//状态
            item.setIsDefault("1");//是否默认
            item.setNum(99999);//库存数量
            item.setSpec("{}");
            setItemValus(goods,item);
            itemMapper.insert(item);
        }

    }

    @Autowired
    private TbBrandMapper brandMapper;
    @Autowired
    private TbItemCatMapper itemCatMapper;
    @Autowired
    private TbSellerMapper sellerMapper;


    private void setItemValus(Goods goods,TbItem item) {
        item.setGoodsId(goods.getGoods().getId());//商品SPU编号
        item.setSellerId(goods.getGoods().getSellerId());//商家编号
        item.setCategoryid(goods.getGoods().getCategory3Id());//商品分类编号（3级）
        item.setCreateTime(new Date());//创建日期
        item.setUpdateTime(new Date());//修改日期

        //品牌名称
        TbBrand brand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
        item.setBrand(brand.getName());
        //分类名称
        TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(goods.getGoods().getCategory3Id());
        item.setCategory(itemCat.getName());

        //商家名称
        TbSeller seller = sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId());
        item.setSeller(seller.getNickName());

        //图片地址（取spu的第一个图片）
        List<Map> imageList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class) ;
        if(imageList.size()>0){
            item.setImage ( (String)imageList.get(0).get("url"));
        }
    }

    /**
     * 插入 SKU 列表数据
     * @param goods
     */
    private void saveItemList(Goods goods){
        if("1".equals(goods.getGoods().getIsEnableSpec())){
            for(TbItem item :goods.getItemList()){
//标题
                String title= goods.getGoods().getGoodsName();
                Map<String,Object> specMap = JSON.parseObject(item.getSpec());
                for(String key:specMap.keySet()){
                    title+=" "+ specMap.get(key);
                }
                item.setTitle(title);
                setItemValus(goods,item);
                itemMapper.insert(item);
            }
        }else{
            TbItem item=new TbItem();
            item.setTitle(goods.getGoods().getGoodsName());//商品 KPU+规格描述串作为SKU 名称
            item.setPrice( goods.getGoods().getPrice() );//价格
            item.setStatus("1");//状态
            item.setIsDefault("1");//是否默认
            item.setNum(99999);//库存数量
            item.setSpec("{}");
            setItemValus(goods,item);
            itemMapper.insert(item);
        }
    }


    /**
     * 修改
     */
    public void update(TbGoods goods) {
        goodsMapper.updateByPrimaryKey(goods);
    }

    /**
     * 修改
     */
    @Override
    public void update(Goods goods){

        TbGoods tbGoods = goods.getGoods();

        tbGoods.setAuditStatus("0");
        goodsMapper.updateByPrimaryKey(tbGoods);
        goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());

        //先删除itemList然后,将更新后的保存到数据库中
        TbItemExample itemExample = new TbItemExample();
        TbItemExample.Criteria criteria = itemExample.createCriteria();
        criteria.andGoodsIdEqualTo(tbGoods.getId());
        itemMapper.deleteByExample(itemExample);
        //添加新的 sku 列表数据
        saveItemList(goods);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public Goods findOne(Long id) {
        Goods goods=new Goods();
        TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
        goods.setGoods(tbGoods);
        TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);
        goods.setGoodsDesc(tbGoodsDesc);
        return goods;

    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            goodsMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbGoodsExample example = new TbGoodsExample();
        Criteria criteria = example.createCriteria();

        if (goods != null) {
            if (goods.getSellerId() != null && goods.getSellerId().length() > 0) {
                //criteria.andSellerIdLike("%" + goods.getSellerId() + "%");
                criteria.andSellerIdEqualTo(goods.getSellerId());
            }
            if (goods.getGoodsName() != null && goods.getGoodsName().length() > 0) {
                criteria.andGoodsNameLike("%" + goods.getGoodsName() + "%");
            }
            if (goods.getAuditStatus() != null && goods.getAuditStatus().length() > 0) {
                criteria.andAuditStatusLike("%" + goods.getAuditStatus() + "%");
            }
            if (goods.getIsMarketable() != null && goods.getIsMarketable().length() > 0) {
                criteria.andIsMarketableLike("%" + goods.getIsMarketable() + "%");
            }
            if (goods.getCaption() != null && goods.getCaption().length() > 0) {
                criteria.andCaptionLike("%" + goods.getCaption() + "%");
            }
            if (goods.getSmallPic() != null && goods.getSmallPic().length() > 0) {
                criteria.andSmallPicLike("%" + goods.getSmallPic() + "%");
            }
            if (goods.getIsEnableSpec() != null && goods.getIsEnableSpec().length() > 0) {
                criteria.andIsEnableSpecLike("%" + goods.getIsEnableSpec() + "%");
            }
            if (goods.getIsDelete() != null && goods.getIsDelete().length() > 0) {
                criteria.andIsDeleteLike("%" + goods.getIsDelete() + "%");
            }

        }

        Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void updateStatus(Long[] ids, String status) {

    }

    @Override
    public List<TbItem> findItemListByGooodsIdandStatus(Long[] ids, String status) {
        return null;
    }

}
