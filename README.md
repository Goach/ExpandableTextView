# ExpandableTextView


 * @author gzc
 * github原始地址
 * https://github.com/Manabu-GT/ExpandableTextView
 *
 * modify author  Goach.zhong
 * 优化的一些地方：
 *  1、mCollapsedHeight 为0问题，因为收起和展开是两个ExpandableTextView,所以就始终为0
 *  2、偶尔出现收起位置不对问题，这个是动画未执行end方法，修改动画即可
 *  3、在RecyclerView使用的时候，展开和收起状态未保存问题
 *  4、TextView不支持<img>标签，解决展示图片问题。
 *  5、展开和收缩实现折叠效果，再文字满一行才换行，不满一行，展开和文字在同一行

