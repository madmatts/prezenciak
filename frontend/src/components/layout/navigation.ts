import { BookmarkCheck, Gift, Heart, List, Users } from '@lucide/vue'
import type { Component } from 'vue'

export interface NavItem {
  /** Route name. */
  name: string
  /** Sidebar label (desktop). */
  labelKey: string
  /** Short bottom-navigation label (mobile). */
  shortLabelKey: string
  icon: Component
}

/** Main navigation (design P1 shell). Mobile shows the first three plus "More"; desktop shows all. */
export const NAV_ITEMS: readonly NavItem[] = [
  { name: 'items', labelKey: 'nav.items', shortLabelKey: 'nav.itemsShort', icon: Gift },
  { name: 'lists', labelKey: 'nav.lists', shortLabelKey: 'nav.listsShort', icon: List },
  { name: 'circles', labelKey: 'nav.circles', shortLabelKey: 'nav.circles', icon: Users },
  { name: 'shared', labelKey: 'nav.shared', shortLabelKey: 'nav.sharedShort', icon: Heart },
  {
    name: 'reservations',
    labelKey: 'nav.reservations',
    shortLabelKey: 'nav.reservations',
    icon: BookmarkCheck,
  },
]

export const BOTTOM_NAV_ITEMS = NAV_ITEMS.filter((item) =>
  ['items', 'lists', 'shared'].includes(item.name),
)
export const MORE_ITEMS = NAV_ITEMS.filter((item) =>
  ['reservations', 'circles'].includes(item.name),
)
