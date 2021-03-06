package org.mightyfrog.android.s4fd.details.tabcontents.attacks

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.mightyfrog.android.s4fd.R
import org.mightyfrog.android.s4fd.data.Move

/**
 * @author Shigehiro Soejima
 */
class AttacksAdapter(var mList: List<Move>, val mListener: AttacksFragment.OnItemClickListener) : RecyclerView.Adapter<AttacksAdapter.AttributeViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = mList.size

    override fun getItemId(position: Int): Long = mList[position].id.toLong()

    override fun onBindViewHolder(holder: AttributeViewHolder?, position: Int) {
        holder?.bind(mList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AttributeViewHolder {
        val vh = AttributeViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.vh_attack, parent, false))
        vh.itemView.setOnClickListener {
            var name = mList[vh.adapterPosition].name
            var counter = 0
            for (c in name.toCharArray()) {
                if (c.isLetter() || c == ' ') {
                    counter++
                    continue
                }
                counter--
                break
            }
            name = name.subSequence(0, counter).toString()
            mListener.onItemClick(name)
        }
        return vh
    }

    fun update(list: List<Move>) {
        mList = list
        notifyDataSetChanged()
    }

    class AttributeViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        private val nameTv = this.itemView?.findViewById(R.id.name) as TextView
        private val typeTv = this.itemView?.findViewById(R.id.type) as TextView
        private val valueTv = this.itemView?.findViewById(R.id.value) as TextView

        fun bind(datum: Move) {
            with(datum) {
                nameTv.text = name
                val hitbox = fromHtml(hitboxActive)
                val faf = fromHtml(firstActionableFrame)
                val damage = fromHtml(baseDamage)
                val angle = fromHtml(angle)
                val bkb = fromHtml(baseKnockBackSetKnockback)
                val lag = fromHtml(landingLag)
                val ac = fromHtml(autoCancel)
                val kbg = fromHtml(knockbackGrowth)
                when (type) {
                    0 -> { // aerials
                        typeTv.text = itemView.context.getString(R.string.type_aerial)
                        valueTv.text = itemView.context.getString(R.string.attack_frame_data_0, hitbox, faf, damage, angle, bkb, lag, ac, kbg)
                    }
                    1 -> { // grounds
                        typeTv.text = itemView.context.getString(R.string.type_ground)
                        if (damage.isEmpty()) { // grabs,dodges
                            valueTv.text = itemView.context.getString(R.string.attack_frame_data_1b, hitbox, faf)
                        } else {
                            valueTv.text = itemView.context.getString(R.string.attack_frame_data_1, hitbox, faf, damage, angle, bkb, kbg)
                        }
                    }
                    2 -> { // specials
                        typeTv.text = itemView.context.getString(R.string.type_special)
                        valueTv.text = itemView.context.getString(R.string.attack_frame_data_1, hitbox, faf, damage, angle, bkb, kbg)
                    }
                    3 -> { // throws
                        typeTv.text = itemView.context.getString(R.string.type_throw)
                        valueTv.text = itemView.context.getString(R.string.attack_frame_data_3, damage, angle, bkb, kbg)
                    }
                    else -> valueTv.text = toString()
                }
            }
        }

        private fun fromHtml(html: String?): Spanned = Html.fromHtml(html)
    }
}