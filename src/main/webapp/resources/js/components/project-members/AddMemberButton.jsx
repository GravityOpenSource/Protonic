import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Button,
  Form,
  Modal,
  notification,
  Radio,
  Select,
  Typography,
} from "antd";
import {
  addMemberToProject,
  getAvailableUsersForProject,
} from "../../apis/projects/members";
import { useDebounce, useResetFormOnCloseModal } from "../../hooks";
import { SPACE_XS } from "../../styles/spacing";
import { PagedTableContext } from "../ant.design/PagedTable";
import { ProjectRolesContext } from "../../contexts/ProjectRolesContext";

const { Option } = Select;
const { Text } = Typography;

/**
 * React component that adds a button to open a modal to add a new member
 * to a project.  User can search for a user but first name, last name or
 * username, and select the project role for the user.
 * @returns {*}
 * @constructor
 */
export function AddMembersButton() {
  /*
  Required a reference to the user select input so that focus can be set
  to it when the window opens.
   */
  const userRef = useRef();
  const { roles } = useContext(ProjectRolesContext);
  const { updateTable } = useContext(PagedTableContext);

  const [userId, setUserId] = useState();
  const [role, setRole] = useState("PROJECT_USER");
  const [visible, setVisible] = useState(false);
  const [query, setQuery] = useState("");
  const debouncedQuery = useDebounce(query, 350);
  const [results, setResults] = useState([]);
  const [form] = Form.useForm();
  useResetFormOnCloseModal({
    form,
    visible,
  });

  useEffect(() => {
    if (visible) {
      setTimeout(() => userRef.current.focus(), 100);
    }
  }, [visible]);

  useEffect(() => {
    if (debouncedQuery) {
      getAvailableUsersForProject(debouncedQuery).then((data) =>
        setResults(data)
      );
    } else {
      setResults([]);
    }
  }, [debouncedQuery]);

  const options = results.map((u) => (
    <Option key={u.identifier}>
      <Text style={{ marginRight: SPACE_XS }}>{u.label}</Text>
      <Text type="secondary">{u.username}</Text>
    </Option>
  ));

  const addUserToProject = () => {
    addMemberToProject({ id: userId, role })
      .then((message) => {
        updateTable();
        notification.success({ message });
        setVisible(false);
      })
      .catch((message) => notification.error({ message }));
  };

  return (
    <>
      <Button onClick={() => setVisible(true)}>
        {i18n("AddMemberButton.label")}
      </Button>
      <Modal
        visible={visible}
        okButtonProps={{ disabled: typeof userId === "undefined" }}
        onCancel={() => setVisible(false)}
        title={i18n("AddMemberButton.modal.title")}
        onOk={addUserToProject}
      >
        <Form layout="vertical" form={form}>
          <Form.Item
            label={i18n("AddMemberButton.modal.user-label")}
            help={i18n("AddMemberButton.modal.user-help")}
            name="user"
          >
            <Select
              ref={userRef}
              showSearch
              notFoundContent={null}
              onSearch={setQuery}
              onChange={setUserId}
              style={{ width: "100%" }}
              value={userId}
              filterOption={false}
            >
              {options}
            </Select>
          </Form.Item>
          <Form.Item label={i18n("AddMemberButton.modal.role")} name="role">
            <Radio.Group
              style={{ display: "flex" }}
              defaultValue={role}
              onChange={(e) => setRole(e.target.value)}
            >
              {roles.map((role) => (
                <Radio.Button key={role.value} value={role.value}>
                  {role.label}
                </Radio.Button>
              ))}
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
