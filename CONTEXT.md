# Sales Visit Registration

This context defines the business language for a team tool that records customer contact activity and lets managers review team activity.

## Language

**Customer Contact**:
A single actual interaction between a sales member and a customer or prospective customer.
_Avoid_: Visit,拜访 when used to mean all contact types

**Planned Contact**:
A sales member's plan to contact one of their customers on a specific date.
_Avoid_: Scheduled visit, task when referring to customer contact planning

**Sales Member Activity Calendar**:
The date-organized view of a **Sales Member**'s **Planned Contacts** and **Customer Contacts**, used to determine today's planned customers, registered plans, unregistered plans, and plan execution counts.
_Avoid_: Task list, schedule when referring to planned/contact registration rules

**Registered Planned Contact**:
A **Planned Contact** for which the sales member has submitted a **Customer Contact** on the planned date.
_Avoid_: Completed task when completion means anything other than submitted contact record

**Unregistered Planned Contact**:
A **Planned Contact** whose planned date has passed without a submitted **Customer Contact**.
_Avoid_: Missed visit

**Unplanned Contact**:
A **Customer Contact** submitted without a matching **Planned Contact** for that day.
_Avoid_: Ad hoc visit

**Contact Method**:
The channel or setting through which a **Customer Contact** happens, captured inside the **Communication Summary** rather than as a separate field.
_Avoid_: Visit type

**Prospective Customer**:
A person or organization that may become a formal customer but has not yet signed an agreement.
_Avoid_: Lead, prospect, account

**Formal Customer**:
A person or organization that has signed an agreement.
_Avoid_: Client, account

**Inactive Customer**:
A customer that a manager has marked as no longer available for new **Planned Contacts** or **Customer Contacts**.
_Avoid_: Deleted customer

**Agreement**:
A paper or electronic contract that has been signed by a customer.
_Avoid_: Verbal agreement, intent, deal

**Agreement Signing Date**:
The date on which an **Agreement** was signed.
_Avoid_: Conversion date, closing date

**Owner**:
The sales member currently responsible for progressing a **Prospective Customer** or **Formal Customer**.
_Avoid_: Private customer holder

**Customer Name**:
The unique business name used to identify a **Prospective Customer** or **Formal Customer**.
_Avoid_: Display name, nickname

**Contact Time**:
The system-recorded time at which a **Customer Contact** is submitted.
_Avoid_: Editable visit time

**Business Date**:
The Asia/Shanghai natural date used by the system when comparing **Planned Contacts** with **Customer Contacts**, showing today's activity, and calculating recent-contact windows.
_Avoid_: Server date, browser date, UTC date when applying contact planning rules

**Communication Summary**:
A free-text summary of the contact method, contacted person, communication content, and conclusion for a **Customer Contact**.
_Avoid_: Notes, remarks

**Attention Level**:
A five-level priority indicating how much attention a customer needs, with higher levels requiring more attention.
_Avoid_: Intent status, signing probability

**Manager**:
A team leader who can review all customers and **Customer Contacts** across the team.
_Avoid_: Admin when referring to sales management

**Sales Member**:
A team member who records **Customer Contacts** for customers they are responsible for.
_Avoid_: User when referring to sales work

**Inactive Sales Member**:
A former or disabled team member who can no longer own customers or submit **Customer Contacts**.
_Avoid_: Deleted user

**User Account**:
The authenticated identity used by a **Sales Member** or **Manager** to access the tool.
_Avoid_: Shared form identity

**Phone Number**:
The unique login identifier for a **User Account**.
_Avoid_: Username

## Relationships

- A **Sales Member** or **Manager** uses exactly one **User Account**
- A **User Account** can have both the **Sales Member** and **Manager** roles
- A **Manager** reviewing team activity includes their own **Sales Member** activity
- A **User Account** has exactly one **Phone Number**
- A **Manager** can change a **Phone Number** if the new number is unique
- A **Customer Contact** is submitted by exactly one sales member
- A **Customer Contact** belongs to exactly one **Prospective Customer** or **Formal Customer**
- A **Customer Contact** has exactly one **Contact Time**
- A **Customer Contact** has exactly one **Communication Summary**
- A **Contact Method** is captured in the **Communication Summary**
- A **Business Date** is derived from **Contact Time** using Asia/Shanghai
- A **Sales Member Activity Calendar** is organized by **Sales Member** and date
- A **Sales Member Activity Calendar** includes **Planned Contacts** and **Customer Contacts**
- A **Planned Contact** belongs to exactly one **Prospective Customer** or **Formal Customer**
- A **Planned Contact** is created by exactly one **Sales Member**
- A **Planned Contact** has exactly one planned date
- A **Planned Contact** becomes a **Registered Planned Contact** when a **Customer Contact** is submitted for it on the planned date
- An **Unplanned Contact** is still a **Customer Contact**
- A **Sales Member** can create **Planned Contacts** only for customers where they are the **Owner**
- A **Planned Contact** can be created for any future date
- A **Sales Member** can have at most one **Planned Contact** for the same customer on the same planned date
- Future **Planned Contacts** can be changed or deleted
- Today's and past **Planned Contacts** cannot be deleted
- A past **Planned Contact** without a **Customer Contact** becomes an **Unregistered Planned Contact**
- A **Planned Contact** is registered by the first **Customer Contact** submitted for its customer on the planned date
- Additional **Customer Contacts** on that planned date do not increase the planned completion count
- A **Prospective Customer** becomes a **Formal Customer** when an **Agreement** is signed
- An **Agreement** has exactly one **Agreement Signing Date**
- A **Prospective Customer** or **Formal Customer** has exactly one current **Owner**
- A customer has exactly one **Attention Level**
- A newly created **Prospective Customer** starts at **Attention Level** one
- A **Customer Name** belongs to at most one customer across active and inactive states
- A **Manager** can review all customers and **Customer Contacts**
- A **Sales Member** can review full details for customers where they are the **Owner**
- A **Sales Member** can submit **Customer Contacts** only for customers where they are the **Owner**
- An **Inactive Customer** cannot receive new **Customer Contacts**
- An **Inactive Customer** cannot receive new **Planned Contacts**
- A **Manager** can change the **Owner** of a customer
- A **Manager** can create a customer and assign its **Owner**
- A **Manager** can change a **Customer Name** if the new name is unique
- A **Manager** can mark a customer as an **Inactive Customer**
- A **Manager** can restore an **Inactive Customer**
- A **Manager** can change any customer's **Attention Level**
- A **Sales Member** who creates a **Prospective Customer** becomes its initial **Owner**
- A **Sales Member** can create only **Prospective Customers**
- A **Sales Member** can change the **Attention Level** for customers where they are the **Owner**
- An **Inactive Sales Member** cannot be an **Owner**
- A **Sales Member** must transfer all owned customers before becoming an **Inactive Sales Member**
- An **Inactive Sales Member** cannot log in

## Example dialogue

> **Dev:** "Should a phone call after an in-person visit be entered as part of the same **Customer Contact**?"
> **Domain expert:** "No. Each actual interaction is its own **Customer Contact**, and the method records whether it was in-person, phone, WeChat, exhibition, or referral."

> **Dev:** "Can a sales member submit a **Customer Contact** before creating the customer?"
> **Domain expert:** "No. They first create or select a **Prospective Customer**, then submit the **Customer Contact** against that customer."

> **Dev:** "Does a **Planned Contact** count as a completed **Customer Contact**?"
> **Domain expert:** "No. A **Planned Contact** is only a plan; a **Customer Contact** is the submitted record after an actual interaction."

> **Dev:** "Does a **Planned Contact** include a purpose or note?"
> **Domain expert:** "No. The first version keeps it to customer and planned date."

> **Dev:** "Does a **Registered Planned Contact** disappear from the sales member's today list?"
> **Domain expert:** "No. It remains visible for the rest of the day as registered."

> **Dev:** "Can a **Sales Member** submit a **Customer Contact** that was not planned for today?"
> **Domain expert:** "Yes, from the customer detail page for a customer they own; the today list itself only shows planned customers."

> **Dev:** "Can a **Sales Member** delete a **Planned Contact** for today?"
> **Domain expert:** "No. Future **Planned Contacts** can be changed or deleted, but today's and past planned contacts remain visible."

> **Dev:** "Does an unregistered **Planned Contact** automatically move to the next day?"
> **Domain expert:** "No. It remains an **Unregistered Planned Contact** for its original planned date."

> **Dev:** "Can one **Planned Contact** have multiple same-day **Customer Contacts**?"
> **Domain expert:** "Yes, but the first **Customer Contact** registers the plan and later same-day records do not increase completion counts."

> **Dev:** "Does verbal intent mean the customer has become a **Formal Customer**?"
> **Domain expert:** "No. Only a signed paper or electronic **Agreement** converts a **Prospective Customer** into a **Formal Customer**."

> **Dev:** "Does signing an **Agreement** change the customer's **Owner**?"
> **Domain expert:** "No. The current **Owner** remains unchanged unless a **Manager** changes it."

> **Dev:** "Can a **Formal Customer** still receive **Planned Contacts** and **Customer Contacts**?"
> **Domain expert:** "Yes. Signing does not remove the customer from future contact planning or contact records."

> **Dev:** "Must a **Sales Member** upload an agreement attachment to mark a customer as signed?"
> **Domain expert:** "No. The first version requires an **Agreement Signing Date** but does not require an attachment."

> **Dev:** "Does registering a signed **Agreement** automatically create a **Customer Contact**?"
> **Domain expert:** "No. Signing status changes do not create **Customer Contacts**."

> **Dev:** "Does registering a signed **Agreement** register a same-day **Planned Contact**?"
> **Domain expert:** "No. A **Planned Contact** is registered only by a submitted **Customer Contact**."

> **Dev:** "Can a team member record a **Customer Contact** for a customer owned by another member?"
> **Domain expert:** "No. In the first version, only the current **Owner** can submit **Customer Contacts** for that customer."

> **Dev:** "Can sales members create two customers with the same **Customer Name** if the phone or address differs?"
> **Domain expert:** "No. The **Customer Name** must be unique, so a duplicate name blocks customer creation."

> **Dev:** "Can a **Sales Member** reuse the **Customer Name** of an **Inactive Customer**?"
> **Domain expert:** "No. The **Customer Name** remains unique across active and inactive customers."

> **Dev:** "What information is required to create a **Prospective Customer**?"
> **Domain expert:** "Only the **Customer Name** is required in the first version."

> **Dev:** "Can a **Sales Member** directly create a **Formal Customer**?"
> **Domain expert:** "No. A **Sales Member** creates **Prospective Customers** and can convert their own customer by registering a signed **Agreement**."

> **Dev:** "Can a **Manager** create a historical **Formal Customer** without an **Agreement Signing Date**?"
> **Domain expert:** "No. Every **Formal Customer** must have an **Agreement Signing Date**, even when entered from historical records."

> **Dev:** "Does a higher **Attention Level** mean only higher signing probability?"
> **Domain expert:** "No. It means the customer needs more attention, including before and after signing."

> **Dev:** "What **Attention Level** does a new **Prospective Customer** start with?"
> **Domain expert:** "Level one."

> **Dev:** "Does a sales member choose the **Contact Time** when submitting a **Customer Contact**?"
> **Domain expert:** "No. The **Contact Time** is the system-recorded submission time."

> **Dev:** "Can a **Sales Member** backfill an old **Customer Contact**?"
> **Domain expert:** "No. The first version does not support backfilled contacts; the **Contact Time** is the submission time."

> **Dev:** "Can a submitted **Communication Summary** be edited?"
> **Domain expert:** "No. Once submitted, a **Customer Contact** is immutable."

> **Dev:** "Is there a special correction workflow for a mistaken **Customer Contact**?"
> **Domain expert:** "No. The first version keeps the original record and uses a new **Customer Contact** to explain the correction."

> **Dev:** "Can a **Manager** delete a **Customer Contact**?"
> **Domain expert:** "No. **Customer Contacts** are never deleted; incorrect customers can be marked as **Inactive Customer** instead."

> **Dev:** "Can an **Inactive Customer** be restored?"
> **Domain expert:** "Yes. A **Manager** can restore it, and the existing **Owner** remains unless the **Manager** changes it."

> **Dev:** "What happens when a **Manager** changes a customer's **Owner**?"
> **Domain expert:** "The new **Owner** can view the full customer history; the previous **Owner** loses full customer access unless they are still a **Manager**."

> **Dev:** "What happens to **Planned Contacts** when a customer changes **Owner**?"
> **Domain expert:** "Today's and future **Planned Contacts** from the previous **Owner** are deleted; past plans remain with their original sales member."

> **Dev:** "Can a **Manager** submit a **Customer Contact** on behalf of a **Sales Member**?"
> **Domain expert:** "No. A **Customer Contact** must be submitted by the **Sales Member** who performed the contact."

## Flagged ambiguities

- "拜访" was used to mean both in-person visits and broader customer interactions — resolved: the canonical term is **Customer Contact**, with in-person visits represented as one **Contact Method**.
- "客户" can mean either unsigned or signed organizations or people — resolved: use **Prospective Customer** before agreement and **Formal Customer** after agreement.
- "达成协议" was ambiguous between intent and contract signing — resolved: an **Agreement** means a signed paper or electronic contract.
- Duplicate customer detection was ambiguous between soft warnings and hard blocking — resolved: duplicate **Customer Name** blocks customer creation.
- **Contact Method** was considered as a structured field — resolved: first version captures it in the **Communication Summary** placeholder instead.
- Collaboration contacts were considered — resolved: first version allows **Sales Members** to submit **Customer Contacts** only for customers where they are the **Owner**.
- "关注程度" was considered as signing probability — resolved: **Attention Level** means how much attention a customer needs, from level one to level five.
