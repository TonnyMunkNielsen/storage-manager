# Build List: Rename Produce Domain to Item Domain

## Goal

Generalize the application domain by renaming every project-owned Produce concept to Item:

- `ProduceType` -> `ItemType`
- `produce_type` -> `item_type`
- `ProduceInstance` -> `ItemInstance`
- `produce_instance` -> `item_instance`
- `produce_type_id` -> `item_type_id`
- `PRODUCE_INSTANCE` -> `ITEM_INSTANCE`

The final runtime application, database schema, URLs, UI labels, log messages, validation messages, exported JSON, and code-level names should use Item terminology.

## Migration Assumption

The current schema has only been deployed locally and may be wiped freely. Use the strict rewrite path:

- Rewrite `src/main/resources/db/migration/V1__Create_initial_schema.sql` directly.
- Do not add a `V2__Rename_produce_to_item.sql` migration for this task.
- Drop/wipe any existing local database and let Flyway apply the rewritten `V1` from scratch.
- After the implementation, no runtime schema object should retain Produce terminology.

## Database Build List

1. Rewrite `src/main/resources/db/migration/V1__Create_initial_schema.sql`:

   - comments: "Create item_type table", "Create item_instance table"
   - tables: `item_type`, `item_instance`
   - column: `item_type_id`
   - foreign keys: references to `item_type` and `item_instance`
   - notification comment: `'ITEM_INSTANCE' or 'STORAGE_BOX'`
   - indexes: `idx_item_instance_item_type_id`, `idx_item_instance_storage_box_id`, `idx_item_instance_best_before_date`, `idx_item_instance_status`
   - triggers: `update_item_type_updated_at`, `update_item_instance_updated_at`

2. Verify Hibernate validates against `item_type`, `item_instance`, and `item_type_id`.

3. Verify no schema object names contain `produce`, including table names, column names, index names, trigger names, and comments in the migration.

## JPA Entity Build List

1. Rename files/classes:

   - `src/main/java/net/tmn/storage_manager/database/jpa/ProduceType.java` -> `ItemType.java`
   - `src/main/java/net/tmn/storage_manager/database/jpa/ProduceInstance.java` -> `ItemInstance.java`
   - `src/main/java/net/tmn/storage_manager/database/jpa/type/ProduceInstanceStatus.java` -> `ItemInstanceStatus.java`

2. Add explicit table names to avoid relying on naming strategy:

   - `@Table(name = "item_type")` on `ItemType`
   - `@Table(name = "item_instance")` on `ItemInstance`

3. In `ItemType`:

   - rename class to `ItemType`
   - rename `List<ProduceInstance> instances` to `List<ItemInstance> instances`
   - change `@OneToMany(mappedBy = "produceType", ...)` to `mappedBy = "itemType"`

4. In `ItemInstance`:

   - rename class to `ItemInstance`
   - update import to `ItemInstanceStatus`
   - rename field `ProduceType produceType` to `ItemType itemType`
   - change validation message to "Item type is required"
   - change join column to `@JoinColumn(name = "item_type_id", nullable = false, updatable = false)`
   - rename `ProduceInstance replacedBy` to `ItemInstance replacedBy`
   - update `getDaysRemaining()` to use `itemType`

5. In `StorageBox.java`:

   - change `List<ProduceInstance> produceInstances` to `List<ItemInstance> itemInstances`
   - update `@OneToMany(mappedBy = "storageBox", ...)` generic type only; `mappedBy` remains `storageBox`

6. In `NotificationTargetType.java`:

   - replace enum value `PRODUCE_INSTANCE` with `ITEM_INSTANCE`
   - because local data may be wiped, no persisted `PRODUCE_INSTANCE` notification rows need to be carried forward

## Repository Build List

1. Rename files/classes:

   - `ProduceTypeRepository.java` -> `ItemTypeRepository.java`
   - `ProduceInstanceRepository.java` -> `ItemInstanceRepository.java`

2. In `ItemTypeRepository`:

   - extend `JpaRepository<ItemType, UUID>`
   - change JPQL to `SELECT it FROM ItemType it ORDER BY it.name`
   - keep `findByName`, `findAllOrderByName`, and `existsByName`

3. In `ItemInstanceRepository`:

   - extend `JpaRepository<ItemInstance, UUID>`
   - update entity graphs from `{"produceType", "storageBox"}` to `{"itemType", "storageBox"}`
   - rename derived methods:
     - `findByProduceTypeIdOrderByBestBeforeDate` -> `findByItemTypeIdOrderByBestBeforeDate`
     - `countByProduceTypeIdAndStatus` -> `countByItemTypeIdAndStatus`
   - rename custom query method:
     - `findActiveInstancesByProduceType` -> `findActiveInstancesByItemType`
   - update JPQL entity and field names:
     - `ProduceInstance` -> `ItemInstance`
     - `pi.produceType` -> `ii.itemType`
     - parameter `produceTypeId` -> `itemTypeId`
   - update status enum type from `ProduceInstanceStatus` to `ItemInstanceStatus`

## Service Build List

1. Rename files/classes:

   - `ProduceTypeService.java` -> `ItemTypeService.java`
   - `ProduceInstanceService.java` -> `ItemInstanceService.java`
   - `ProduceTypeTransferData.java` -> `ItemTypeTransferData.java`

2. In `ItemTypeService`:

   - inject `ItemTypeRepository itemTypeRepository`
   - rename public methods:
     - `getAllProduceTypes` -> `getAllItemTypes`
     - `getProduceTypeById` -> `getItemTypeById`
     - `getProduceTypeByName` -> `getItemTypeByName`
     - `exportProduceTypes` -> `exportItemTypes`
     - `importProduceTypes` -> `importItemTypes`
     - `createProduceType` -> `createItemType`
     - `updateProduceType` -> `updateItemType`
     - `deleteProduceType` -> `deleteItemType`
   - update exception and validation messages from "produce type" to "item type"
   - update local variable names from `produceType` to `itemType`

3. In `ItemTypeTransferData`:

   - record name: `ItemTypeTransferData`
   - nested record name: `ItemTypeRecord`
   - top-level JSON field: `itemTypes`
   - constructor parameter: `List<ItemTypeRecord> itemTypes`
   - do not add compatibility aliases for old exports with a `produceTypes` JSON field

4. In `ItemInstanceService`:

   - inject `ItemInstanceRepository itemInstanceRepository`
   - inject `ItemTypeRepository itemTypeRepository`
   - rename public methods:
     - `getAllProduceInstances` -> `getAllItemInstances`
     - `getActiveProduceInstances` -> `getActiveItemInstances`
     - `getProduceInstanceById` -> `getItemInstanceById`
     - `getProduceInstancesByProduceType` -> `getItemInstancesByItemType`
     - `getActiveProduceInstancesByProduceType` -> `getActiveItemInstancesByItemType`
     - `getExpiredProduceInstances` -> `getExpiredItemInstances`
     - `getProduceInstancesExpiringBetween` -> `getItemInstancesExpiringBetween`
     - `createProduceInstance` -> `createItemInstance`
     - `updateProduceInstance` -> `updateItemInstance`
     - `replaceProduceInstance` -> `replaceItemInstance`
     - `deleteProduceInstance` -> `deleteItemInstance`
   - rename private helper `resolveProduceType` -> `resolveItemType`
   - update all errors/logs/comments to "item" / "item type"
   - update status enum to `ItemInstanceStatus`

5. In `NotificationService`:

   - inject `ItemInstanceRepository itemInstanceRepository`
   - import `ItemInstance` and `ItemType`
   - rename method `getNotificationsByProduceInstance` -> `getNotificationsByItemInstance`
   - replace `NotificationTargetType.PRODUCE_INSTANCE` with `NotificationTargetType.ITEM_INSTANCE`
   - update log messages and notification messages:
     - "Checking for items requiring notifications..."
     - "Item '%s' has expired..."
     - "Item '%s' will expire..."
     - "Created expired notification for item..."
   - rename local variables from `produceType` to `itemType`

6. In `ScheduledTaskService`:

   - inject `ItemInstanceService itemInstanceService`
   - rename `checkExpiredProduces` -> `checkExpiredItems`
   - update logs to "expired items" and "scheduled item check"

7. In `DatabaseBackupService`:

   - update stale comment `jdbc:postgresql://localhost:5432/producedb` to an item/general example, such as `storagemanagerdb`

## REST/API Build List

1. In `ImageController.java`:

   - inject `ItemTypeRepository itemTypeRepository`
   - rename method `getProduceTypeImage` -> `getItemTypeImage`
   - change route from `/api/images/produce-type/{id}` to `/api/images/item-type/{id}`
   - update comments and local variables to "item type"

2. Do not keep a deprecated compatibility route for `/api/images/produce-type/{id}`.

## Vaadin UI Build List

1. Rename files/classes/routes/page titles:

   - `ProduceTypesView.java` -> `ItemTypesView.java`
   - `ProduceInstancesView.java` -> `ItemInstancesView.java`
   - route `produce-types` -> `item-types`
   - route `produce-instances` -> `item-instances`
   - page titles "Produce Types" -> "Item Types", "Produce Instances" -> "Item Instances"

2. In `MainLayout.java`:

   - title "Produce Manager" -> "Storage Manager" or "Item Manager"
   - nav "Produce Types" -> "Item Types"
   - nav "Produces" -> "Items"
   - nav targets to `ItemTypesView.class` and `ItemInstancesView.class`

3. In `VaadinViewUtils.java`:

   - import `ItemInstance` and `ItemType`
   - rename `produceTypeName` -> `itemTypeName`
   - update method parameters to `ItemInstance itemInstance`
   - change `hasImage(ProduceType)` -> `hasImage(ItemType)`
   - change `imageUrl(ProduceType)` -> `imageUrl(ItemType)` and return `/api/images/item-type/{id}`
   - change `thumbnail(ProduceType)` -> `thumbnail(ItemType)`

4. In `DashboardView.java`:

   - inject `ItemInstanceService itemInstanceService`
   - update list types to `List<ItemInstance>`
   - rename local variables: `activeItems`, `expiredItems`, `expiringItems`
   - update labels:
     - "Active Produces" -> "Active Items"
     - "Expired Produces" -> "Expired Items"
     - "No expired produce instances." -> "No expired item instances."
     - "No produce instances expiring soon." -> "No item instances expiring soon."
     - "Produces Expiring Soon" -> "Items Expiring Soon"
     - "Produce Type" -> "Item Type"
   - update grid helper names from `baseProduceGrid` to `baseItemGrid`

5. In `ItemTypesView.java`:

   - use `ItemType`, `ItemTypeService`, `ItemTypeTransferData`
   - update UI strings:
     - "Item Types"
     - "Add Item Type"
     - "No item types found."
     - "Add Item Type" / "Edit Item Type"
     - "Item type created/updated/deleted."
     - "Import item types"
     - "Importing creates new item types..."
     - "Imported X item type(s)."
   - update export fallback error to "Failed to export item types"
   - update export filename from `produce-types-...json` to `item-types-...json`
   - rename local variables from `produceType` / `editedProduceType` to `itemType` / `editedItemType`

6. In `ItemInstancesView.java`:

   - use `ItemInstance`, `ItemType`, `ItemInstanceStatus`, `ItemInstanceService`, `ItemTypeService`
   - update field names:
     - combo box `produceType` -> `itemType`
     - preview div `produceTypePreview` -> `itemTypePreview`
     - list `produceTypes` -> `itemTypes`
   - update UI strings:
     - "Item Instances"
     - "Add Item Instance"
     - "No item instances found."
     - combo label "Item type"
     - validation "Item type is required"
     - dialog "Add Item Instance" / "Edit Item Instance"
     - "Item instance created/updated/deleted."
     - delete header "Delete item instance"
     - column "Item Type"
   - update helper names:
     - `newProduceInstance` -> `newItemInstance`
     - `findMatchingProduceType` -> `findMatchingItemType`
     - `updateProduceTypePreview` -> `updateItemTypePreview`

## Configuration Build List

1. In `src/main/resources/application.yaml`:

   - update comment "Produce type imports..." -> "Item type imports..."
   - replace logging category `com.produce: DEBUG` with a real project package category such as `net.tmn.storage_manager: DEBUG`, or remove the stale category

## Search Checklist

Run these searches after the rename and fix every project-owned hit:

```powershell
rg -n "Produce|produce|PRODUCE|ProduceType|produce_type|PRODUCE_TYPE" src
rg -n "ItemType|item_type|ITEM_INSTANCE|ItemInstance|item_instance" src
```

Expected residuals:

- None in Java, Vaadin routes, REST paths, application config, or new migrations.
- None in `V1__Create_initial_schema.sql`; it should create the Item schema directly.
- `LICENSE` contains unrelated English words such as "produce"; ignore it.

## Verification Build List

1. Format code:

   ```powershell
   .\gradlew.bat spotlessApply
   ```

2. Run tests/build:

   ```powershell
   .\gradlew.bat test
   .\gradlew.bat build
   ```

3. Validate migrations against a fresh PostgreSQL database:

   - Start the app or run the boot test profile with Flyway enabled.
   - Confirm Flyway applies the rewritten `V1`.
   - Confirm final tables are `item_type`, `item_instance`, `storage_box`, `notification`, `app_user`.
   - Confirm no `produce_type` or `produce_instance` tables remain.

4. Validate the local wipe/recreate flow:

   - Drop or wipe the local database.
   - Start the application with Flyway enabled.
   - Confirm only the rewritten `V1` is applied.
   - Confirm foreign keys and indexes are valid.

5. Run the application and smoke test:

   - Dashboard loads.
   - Item Types view lists, creates, edits, deletes, imports, exports, and displays images.
   - Items view lists, creates, edits, deletes, and displays item type image preview.
   - `/api/images/item-type/{id}` returns image bytes for an item type with image data.
   - Notifications are created for expired/expiring items and render target display correctly.

## Risk Notes

- JPQL uses entity and Java field names, not table and column names. After class and field renames, JPQL must use `ItemInstance` and `itemType`.
- Spring Data derived queries depend on Java field names. Methods containing `ProduceTypeId` must become `ItemTypeId` after the field is renamed.
- Entity graph attribute paths also depend on Java field names. `produceType` must become `itemType`.
- Enum values are persisted as strings. Because local data may be wiped, do not carry forward `PRODUCE_INSTANCE`; rewritten schema and new data should only use `ITEM_INSTANCE`.
- REST path changes can break cached image URLs and external clients. For this task, do not keep Produce-named compatibility routes.
- Exported JSON field names will change if `produceTypes` becomes `itemTypes`. For this task, old Produce-named export files are not a compatibility target.
